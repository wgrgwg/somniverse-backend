package dev.wgrgwg.somniverse.global.idempotency.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class IdempotencyKeys {

    public static final String PREFIX = "IDEM:";

    public static String build(String userId, String method, String path, String idemKey) {
        return PREFIX + userId + ":" + method + ":" + normalizePath(path) + ":" + idemKey;
    }

    public static String normalizePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }

        String path = rawPath;

        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }

        path = collapseSlashes(path);

        if (path.length() > 1) {
            boolean endsWithSlash = path.charAt(path.length() - 1) == '/';
            if (endsWithSlash) {
                path = path.substring(0, path.length() - 1);
            }
        }

        return path;
    }

    private static String collapseSlashes(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean prevSlash = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/') {
                if (!prevSlash) {
                    sb.append(c);
                }
                prevSlash = true;
            } else {
                sb.append(c);
                prevSlash = false;
            }
        }
        return sb.toString();
    }
}
