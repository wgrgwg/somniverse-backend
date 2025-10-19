package dev.wgrgwg.somniverse.global.idempotency.filter;

import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.global.errorcode.IdempotencyErrorCode;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.global.idempotency.model.IdempotencyRecord;
import dev.wgrgwg.somniverse.global.idempotency.model.IdempotencyState;
import dev.wgrgwg.somniverse.global.idempotency.store.IdempotencyRepository;
import dev.wgrgwg.somniverse.global.idempotency.util.IdempotencyHashUtil;
import dev.wgrgwg.somniverse.global.idempotency.util.IdempotencyKeys;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> HEADER_WHITELIST = Set.of("Location", "Content-Location",
        "ETag", "Cache-Control", "Vary", "Last-Modified");

    private final IdempotencyRepository idempotencyRepository;
    private final IdempotencyHashUtil hashUtil;
    private final AppProperties appProperties;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (!isIncludedPath(request)) {
            chain.doFilter(request, response);
            return;
        }

        final String idemKey = request.getHeader("Idempotency-Key");
        if (idemKey == null || idemKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        ReusableRequestWrapper wrappedRequest = new ReusableRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String requestHash = hashUtil.hashBody(wrappedRequest.getCachedBody(),
            request.getContentType());

        String userScope = resolveUserIdOrRespondUnauthorized(wrappedRequest, wrappedResponse);
        if (userScope == null) {
            wrappedResponse.copyBodyToResponse();
            return;
        }

        String redisKey = IdempotencyKeys.build(userScope, request.getMethod(),
            request.getRequestURI(), idemKey);

        Duration inProgressTtl = Duration.ofSeconds(
            appProperties.getIdempotency().getInProgressTtl());
        Duration completedTtl = Duration.ofSeconds(appProperties.getIdempotency().getTtlSeconds());

        IdempotencyRecord rec = IdempotencyRecord.inProgress(requestHash);

        boolean acquired;
        try {
            acquired = idempotencyRepository.setIfAbsent(redisKey, rec, inProgressTtl);
        } catch (RuntimeException e) {
            log.warn("[IDEM] repository error: {}", e.getMessage());
            chain.doFilter(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
            return;
        }

        if (acquired) {
            try {
                chain.doFilter(wrappedRequest, wrappedResponse);

                int status = wrappedResponse.getStatus();
                String charset = Optional.ofNullable(wrappedResponse.getCharacterEncoding())
                    .orElse(StandardCharsets.UTF_8.name());

                String body = null;
                if (status != HttpStatus.NO_CONTENT.value()) {
                    body = new String(wrappedResponse.getContentAsByteArray(), charset);
                }

                String contentType = null;
                if (status != HttpStatus.NO_CONTENT.value()) {
                    contentType = wrappedResponse.getContentType();
                }

                Map<String, List<String>> headers = extractReplayHeaders(wrappedResponse);

                persistSnapshotWithFailOpen(redisKey, rec, status, body, contentType, headers,
                    completedTtl);
            } catch (Throwable t) {
                saveFailedSnapshotBestEffort(redisKey, rec);
                throw t;
            } finally {
                wrappedResponse.copyBodyToResponse();
            }
            return;
        }

        Optional<IdempotencyRecord> existingOpt;

        try {
            existingOpt = idempotencyRepository.get(redisKey);
        } catch (RuntimeException e) {
            log.warn("[IDEM] fail-open: get skipped: {}", e.getMessage());
            chain.doFilter(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
            return;
        }

        if (existingOpt.isEmpty()) {
            chain.doFilter(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
            return;
        }

        IdempotencyRecord existing = existingOpt.get();

        boolean same = existing.matchesHash(requestHash);
        if (!same) {
            resolveException(wrappedRequest, wrappedResponse,
                new CustomException(IdempotencyErrorCode.IDEMPOTENCY_CONFLICT));
            wrappedResponse.copyBodyToResponse();
            return;
        }

        boolean isCompleted = IdempotencyState.COMPLETED.equals(existing.state());
        if (isCompleted) {
            Integer existingStatus = existing.responseStatus();
            int replayStatus = Optional.ofNullable(existingStatus).orElse(HttpStatus.OK.value());
            wrappedResponse.setStatus(replayStatus);

            Map<String, List<String>> stored = existing.responseHeaders();
            if (stored != null) {
                stored.forEach((name, values) -> {
                    if (values != null) {
                        for (String v : values) {
                            wrappedResponse.addHeader(name, v);
                        }
                    }
                });
            }

            if (replayStatus != HttpStatus.NO_CONTENT.value()) {
                String ct = existing.responseContentType();
                if (ct != null) {
                    wrappedResponse.setContentType(ct);
                }
                String rb = existing.responseBody();
                if (rb != null) {
                    try {
                        wrappedResponse.getWriter().write(rb);
                    } catch (IllegalStateException ise) {
                        wrappedResponse.getOutputStream()
                            .write(rb.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            wrappedResponse.copyBodyToResponse();
            return;
        }

        wrappedResponse.setHeader(HttpHeaders.RETRY_AFTER,
            String.valueOf(appProperties.getIdempotency().getRetryAfterSeconds()));
        resolveException(wrappedRequest, wrappedResponse,
            new CustomException(IdempotencyErrorCode.IDEMPOTENCY_IN_PROGRESS));
        wrappedResponse.copyBodyToResponse();
    }


    private boolean isIncludedPath(HttpServletRequest request) {
        List<String> includePaths = appProperties.getIdempotency().getIncludePaths();

        if (CollectionUtils.isEmpty(includePaths)) {
            return false;
        }

        String uri = request.getRequestURI();
        for (String prefix : includePaths) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private String resolveUserIdOrRespondUnauthorized(HttpServletRequest request,
        HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            resolveException(request, response,
                new CustomException(MemberErrorCode.TOKEN_NOT_FOUND));
            return null;
        }
        if (!authentication.isAuthenticated()) {
            resolveException(request, response,
                new CustomException(MemberErrorCode.TOKEN_NOT_FOUND));
            return null;
        }

        Object principal = authentication.getPrincipal();
        boolean isCustom = principal instanceof CustomUserDetails;
        if (!isCustom) {
            resolveException(request, response,
                new CustomException(MemberErrorCode.MEMBER_FOR_TOKEN_NOT_FOUND));
            return null;
        }

        CustomUserDetails userDetails = (CustomUserDetails) principal;
        if (userDetails.getMember() == null) {
            resolveException(request, response,
                new CustomException(MemberErrorCode.MEMBER_FOR_TOKEN_NOT_FOUND));
            return null;
        }
        if (userDetails.getMember().getId() == null) {
            resolveException(request, response,
                new CustomException(MemberErrorCode.MEMBER_FOR_TOKEN_NOT_FOUND));
            return null;
        }

        return String.valueOf(userDetails.getMember().getId());
    }

    private boolean isReplayableHeader(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals("content-type") || lower.equals("content-length") || lower.equals(
            "set-cookie") || lower.equals("cookie") || lower.equals("authorization")
            || lower.equals("www-authenticate") || lower.equals("proxy-authenticate")) {
            return false;
        }

        return HEADER_WHITELIST.stream().anyMatch(allow -> allow.equalsIgnoreCase(name));
    }

    private Map<String, List<String>> extractReplayHeaders(ContentCachingResponseWrapper res) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String name : res.getHeaderNames()) {
            if (!isReplayableHeader(name)) {
                continue;
            }
            List<String> values = new ArrayList<>(res.getHeaders(name));
            if (!values.isEmpty()) {
                out.put(name, values);
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        return out;
    }

    private void setWithFailOpen(String redisKey, IdempotencyRecord rec, Duration ttl) {
        try {
            idempotencyRepository.set(redisKey, rec, ttl);
        } catch (RuntimeException e) {
            log.warn("[IDEM] fail-open: set skipped. key={}, state={}, reason={}", redisKey,
                rec.state(), e.getMessage());
        }
    }

    private void persistSnapshotWithFailOpen(String redisKey, IdempotencyRecord base, int status,
        String body, String contentType, Map<String, List<String>> headers, Duration completedTtl) {

        if (status < 500) {
            IdempotencyRecord completed = base.toCompleted(status, body, contentType, headers);
            setWithFailOpen(redisKey, completed, completedTtl);
        }

        if (status >= 500) {
            IdempotencyRecord failed = base.toFailed();
            setWithFailOpen(redisKey, failed, Duration.ofSeconds(5));
        }
    }

    private void saveFailedSnapshotBestEffort(String redisKey,
        IdempotencyRecord idempotencyRecord) {
        try {
            idempotencyRepository.set(redisKey, idempotencyRecord.toFailed(),
                Duration.ofSeconds(5));
        } catch (RuntimeException e) {
            log.warn("[IDEM] mark FAILED skipped (fail-open). key={}, reason={}", redisKey,
                e.getMessage());
        }
    }

    private void resolveException(HttpServletRequest request, HttpServletResponse response,
        Exception ex) {
        handlerExceptionResolver.resolveException(request, response, null, ex);
    }
}
