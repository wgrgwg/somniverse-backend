package dev.wgrgwg.somniverse.global.ratelimit.policy;

import io.github.bucket4j.BucketConfiguration;
import java.util.List;
import java.util.Set;

public record CompiledPolicy(
    String name,
    List<String> paths,
    Set<String> methods,
    KeyStrategy strategy,
    BucketConfiguration configuration
) {

}
