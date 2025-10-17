package dev.wgrgwg.somniverse.global.idempotency.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.global.idempotency.model.IdempotencyRecord;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper om;

    public boolean setIfAbsent(String key, IdempotencyRecord rec, Duration ttl) {
        String json = write(rec);

        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, json, ttl));
    }

    public Optional<IdempotencyRecord> get(String key) {
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        return Optional.of(read(json));
    }

    public void set(String key, IdempotencyRecord rec, Duration ttl) {
        redisTemplate.opsForValue().set(key, write(rec), ttl);
    }

    private String write(IdempotencyRecord r) {
        try {
            return om.writeValueAsString(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IdempotencyRecord read(String s) {
        try {
            return om.readValue(s, IdempotencyRecord.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
