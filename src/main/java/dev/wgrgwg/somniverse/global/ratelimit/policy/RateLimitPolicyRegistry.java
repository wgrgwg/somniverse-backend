package dev.wgrgwg.somniverse.global.ratelimit.policy;

import static java.util.stream.Collectors.toCollection;

import dev.wgrgwg.somniverse.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class RateLimitPolicyRegistry {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final List<String> whitelistPaths;
    @Getter
    private final boolean enabled;
    @Getter
    private final boolean addHeaders;
    private final List<CompiledPolicy> compiledPolicies;

    public RateLimitPolicyRegistry(AppProperties appProperties) {
        AppProperties.RateLimit rateLimit = appProperties.getRateLimit();
        this.enabled = rateLimit.isEnabled();
        this.addHeaders = rateLimit.isAddHeaders();
        this.whitelistPaths = rateLimit.getWhitelistPaths();
        this.compiledPolicies = rateLimit.getPolicies().stream().map(this::compile).toList();
    }

    public boolean isWhitelisted(String uri) {
        return whitelistPaths.stream().anyMatch(p -> antPathMatcher.match(p, uri));
    }

    public Optional<MatchedPolicy> match(String uri, String httpMethod) {
        String methodUpper;
        if (httpMethod != null) {
            methodUpper = httpMethod.toUpperCase(Locale.ROOT);
        } else {
            methodUpper = "";
        }

        return compiledPolicies.stream()
            .filter(cp -> cp.methods().isEmpty() || cp.methods().contains(methodUpper))
            .filter(cp -> cp.paths().stream().anyMatch(pat -> antPathMatcher.match(pat, uri)))
            .findFirst().map(cp -> new MatchedPolicy(cp.strategy(), cp.configuration(), cp.name()));

    }

    private CompiledPolicy compile(AppProperties.RateLimit.Policy policy) {
        String name = policy.getName();

        List<String> paths = policy.getPaths();

        Set<String> methods = policy.getMethods().stream().map(s -> s.toUpperCase(Locale.ROOT))
            .collect(toCollection(LinkedHashSet::new));

        KeyStrategy strategy = KeyStrategy.from(policy.getKeyStrategy());
        BucketConfiguration configuration = buildBucketConfiguration(policy);

        return new CompiledPolicy(name, paths, methods, strategy, configuration);
    }

    private BucketConfiguration buildBucketConfiguration(AppProperties.RateLimit.Policy policy) {
        List<Bandwidth> limits = policy.getLimits().stream().map(
            l -> Bandwidth.builder().capacity(l.getCapacity())
                .refillGreedy(l.getCapacity(), l.getRefill()).build()).toList();

        return new BucketConfiguration(limits);
    }

}
