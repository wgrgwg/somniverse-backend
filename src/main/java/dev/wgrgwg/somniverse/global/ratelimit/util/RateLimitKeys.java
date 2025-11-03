package dev.wgrgwg.somniverse.global.ratelimit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitKeys {

    public static final String NAMESPACE = "RATELIM";
    public static final char SEP = ':';
    public static final String SEG_IPUA = "IPUA";

    public static String userBucket(String userKey, String policy) {
        return NAMESPACE + SEP + userKey + SEP + policy;
    }

    public static String ipUaBucket(String ip, String uaHash, String policy) {
        return NAMESPACE + SEP + SEG_IPUA + SEP + ip + SEP + uaHash + SEP + policy;
    }
}