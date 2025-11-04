package dev.wgrgwg.somniverse.global.ratelimit.policy;

public enum KeyStrategy {
    USER, IP_UA;

    public static KeyStrategy from(String v) {
        if (v == null) {
            return IP_UA;
        }

        String s = v.trim().toLowerCase();
        if (s.equals("user")) {
            return USER;
        }

        return IP_UA;
    }
}
