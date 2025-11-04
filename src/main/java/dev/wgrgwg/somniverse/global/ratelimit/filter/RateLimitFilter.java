package dev.wgrgwg.somniverse.global.ratelimit.filter;

import dev.wgrgwg.somniverse.global.errorcode.RateLimitErrorCode;
import dev.wgrgwg.somniverse.global.ratelimit.key.RateLimitKeyResolver;
import dev.wgrgwg.somniverse.global.ratelimit.policy.KeyStrategy;
import dev.wgrgwg.somniverse.global.ratelimit.policy.MatchedPolicy;
import dev.wgrgwg.somniverse.global.ratelimit.policy.RateLimitPolicyRegistry;
import dev.wgrgwg.somniverse.global.ratelimit.util.RateLimitKeys;
import dev.wgrgwg.somniverse.global.ratelimit.util.RateLimitResponseWriter;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<String> proxyManager;
    private final RateLimitPolicyRegistry registry;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimitResponseWriter rateLimitResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        if (!registry.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (registry.isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<MatchedPolicy> matchedPolicy = registry.match(uri, request.getMethod());
        if (matchedPolicy.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        MatchedPolicy policy = matchedPolicy.get();

        String bucketKey = computeBucketKey(request, policy.keyStrategy(), policy.name());

        BucketProxy bucket = proxyManager.getProxy(bucketKey, policy::bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            rateLimitResponseWriter.writeSuccessHeaders(response, probe.getRemainingTokens(),
                registry.isAddHeaders());
            filterChain.doFilter(request, response);
            return;
        }

        long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        rateLimitResponseWriter.writeRetryAfter(response, RateLimitErrorCode.TOO_MANY_REQUESTS,
            seconds, 0L, policy.name(), registry.isAddHeaders());
    }

    private String computeBucketKey(HttpServletRequest request, KeyStrategy keyStrategy,
        String policyName) {

        if (keyStrategy == KeyStrategy.USER) {
            String userKey = keyResolver.resolveUserKeyOrNull();

            if (userKey != null && !userKey.isBlank()) {
                return RateLimitKeys.userBucket(userKey, policyName);
            }
        }

        String ip = keyResolver.resolveClientIp(request);
        String ua = keyResolver.resolveUserAgentHash(request);

        return RateLimitKeys.ipUaBucket(ip, ua, policyName);
    }
}
