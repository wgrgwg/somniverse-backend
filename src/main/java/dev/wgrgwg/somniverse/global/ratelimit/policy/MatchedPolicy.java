package dev.wgrgwg.somniverse.global.ratelimit.policy;

import io.github.bucket4j.BucketConfiguration;

public record MatchedPolicy(
    KeyStrategy keyStrategy,
    BucketConfiguration bucketConfiguration,
    String name
) {

}
    