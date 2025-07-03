package dev.wgrgwg.somniverse.member.domain;

public enum Provider {
    LOCAL, GOOGLE, NAVER;

    public static Provider fromString(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> GOOGLE;
            case "naver" -> NAVER;
            default -> LOCAL;
        };
    }
}
