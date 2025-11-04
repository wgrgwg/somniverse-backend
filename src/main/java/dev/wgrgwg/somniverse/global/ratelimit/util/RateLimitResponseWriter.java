package dev.wgrgwg.somniverse.global.ratelimit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import dev.wgrgwg.somniverse.global.ratelimit.dto.RateLimitBody;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitResponseWriter {

    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_RATELIMIT = "RateLimit";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_PRAGMA = "Pragma";
    private static final String HEADER_EXPIRES = "Expires";

    private static final String CACHE_NO_STORE = "no-store";
    private static final String PRAGMA_NO_CACHE = "no-cache";
    private static final String EXPIRES_ZERO = "0";

    private static final String RL_REMAINING = "remaining=";
    private static final String RL_RESET = ", reset=";

    private static final String CT_JSON_UTF8 = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

    private final ObjectMapper objectMapper;

    public void writeRetryAfter(HttpServletResponse response,
        ErrorCode errorCode,
        long retryAfterSeconds,
        long remaining,
        String policy,
        boolean addHeaders) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        final long waitSeconds = Math.max(1L, retryAfterSeconds);
        final long remainingSafe = Math.max(0L, remaining);

        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(waitSeconds));

        if (addHeaders) {
            String rateLimitValue = RL_REMAINING + remainingSafe + RL_RESET + waitSeconds;
            response.setHeader(HEADER_RATELIMIT, rateLimitValue);
            response.setHeader(HEADER_CACHE_CONTROL, CACHE_NO_STORE);
            response.setHeader(HEADER_PRAGMA, PRAGMA_NO_CACHE);
            response.setHeader(HEADER_EXPIRES, EXPIRES_ZERO);
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(CT_JSON_UTF8);

        ApiResponseDto<RateLimitBody> body = ApiResponseDto.error(
            errorCode.getMessage(),
            new RateLimitBody(waitSeconds, remainingSafe, policy),
            errorCode.getCode()
        );

        objectMapper.writeValue(response.getWriter(), body);
        response.flushBuffer();
    }

    public void writeSuccessHeaders(HttpServletResponse response, long remaining,
        boolean addHeaders) {
        if (!addHeaders) {
            return;
        }

        if (response.isCommitted()) {
            return;
        }

        final long remainingSafe = Math.max(0L, remaining);
        response.setHeader(HEADER_RATELIMIT, RL_REMAINING + remainingSafe + RL_RESET + 0);
    }
}
