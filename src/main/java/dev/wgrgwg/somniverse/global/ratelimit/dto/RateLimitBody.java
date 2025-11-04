package dev.wgrgwg.somniverse.global.ratelimit.dto;

public record RateLimitBody(long retryAfterSeconds, long remaining, String policy) {

}
