package dev.wgrgwg.somniverse.global.idempotency.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.wgrgwg.somniverse.global.util.HashUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyHashUtil {

    private final ObjectMapper om;

    public String canonicalizeJson(byte[] body) {
        if (body == null) {
            return "";
        }
        if (body.length == 0) {
            return "";
        }
        try {
            JsonNode node = om.readTree(body);
            JsonNode normalized = normalize(node);
            return om.writeValueAsString(normalized);
        } catch (Exception e) {
            return new String(body, StandardCharsets.UTF_8).trim();
        }
    }

    public String hashBody(byte[] body) {
        return HashUtil.sha256(canonicalizeJson(body));
    }

    public String hashBody(byte[] body, String contentType) {
        if (body == null) {
            return HashUtil.sha256("");
        }
        if (body.length == 0) {
            return HashUtil.sha256("");
        }
        if (contentType != null) {
            String lc = contentType.toLowerCase(Locale.ROOT);
            if (lc.contains("json")) {
                String canon = canonicalizeJson(body);
                return HashUtil.sha256(canon);
            }
        }
        return sha256Hex(body);
    }

    private JsonNode normalize(JsonNode node) {
        if (node == null) {
            return om.getNodeFactory().nullNode();
        }
        if (node.isObject()) {
            ObjectNode out = om.createObjectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            for (String name : names) {
                JsonNode child = normalize(node.get(name));
                out.set(name, child);
            }
            return out;
        }
        if (node.isArray()) {
            ArrayNode arr = om.createArrayNode();
            for (JsonNode el : node) {
                arr.add(normalize(el));
            }
            return arr;
        }
        return node;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                int hi = (b >>> 4) & 0x0F;
                int lo = b & 0x0F;
                sb.append(Character.forDigit(hi, 16));
                sb.append(Character.forDigit(lo, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
