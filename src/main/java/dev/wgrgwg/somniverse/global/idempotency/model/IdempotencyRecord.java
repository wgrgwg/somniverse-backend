package dev.wgrgwg.somniverse.global.idempotency.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IdempotencyRecord(
    IdempotencyState state,
    String requestHash,
    Integer responseStatus,
    String responseBody,
    String responseContentType,
    Map<String, List<String>> responseHeaders,
    long createdAt
) {

    public static IdempotencyRecord inProgress(String requestHash) {
        return new IdempotencyRecord(
            IdempotencyState.IN_PROGRESS,
            requestHash,
            null,
            null,
            null,
            null,
            System.currentTimeMillis()
        );
    }

    public IdempotencyRecord toCompleted(int status,
        String body,
        String contentType,
        Map<String, List<String>> headers) {
        return new IdempotencyRecord(
            IdempotencyState.COMPLETED,
            this.requestHash,
            status,
            body,
            contentType,
            normalizeHeaders(headers),
            this.createdAt
        );
    }

    public IdempotencyRecord toFailed() {
        return new IdempotencyRecord(
            IdempotencyState.FAILED,
            this.requestHash,
            null,
            null,
            null,
            null,
            this.createdAt
        );
    }

    public boolean matchesHash(String otherHash) {
        return Objects.equals(this.requestHash, otherHash);
    }

    private static Map<String, List<String>> normalizeHeaders(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }

        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e == null) {
                continue;
            }
            String name = e.getKey();
            if (name == null) {
                continue;
            }
            List<String> values = e.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            out.put(name, List.copyOf(values));
        }

        if (out.isEmpty()) {
            return null;
        }
        return out;
    }
}
