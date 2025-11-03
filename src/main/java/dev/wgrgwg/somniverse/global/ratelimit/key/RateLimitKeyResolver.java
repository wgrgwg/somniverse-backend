package dev.wgrgwg.somniverse.global.ratelimit.key;

import dev.wgrgwg.somniverse.global.util.HashUtil;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyResolver {

    public static final String USER_KEY_PREFIX = "USR:";
    public static final String XFF = "X-Forwarded-For";
    public static final String HDR_USER_AGENT = "User-Agent";

    public static final String UA_NONE = "UA_NONE";

    public String resolveUserKeyOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            Long id = customUserDetails.getMember().getId();
            if (id != null) {
                return USER_KEY_PREFIX + id;
            }
        }

        return null;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader(XFF);

        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            String first = xff;

            if (comma >= 0) {
                first = xff.substring(0, comma);
            }

            first = first.trim();

            if (!first.isEmpty()) {
                return first;
            }
        }

        return request.getRemoteAddr();
    }

    public String resolveUserAgentHash(HttpServletRequest request) {
        String ua = request.getHeader(HDR_USER_AGENT);

        if (ua == null || ua.isBlank()) {
            return UA_NONE;
        }

        String hex = HashUtil.sha256(ua.trim());
        return hex.substring(0, 12);
    }
}
