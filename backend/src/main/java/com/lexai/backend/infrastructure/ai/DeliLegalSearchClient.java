package com.lexai.backend.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliLegalSearchClient {

    private static final String ENDPOINT = "https://openapi.delilegal.com/api/qa/v3/search/queryListLaw";

    private final String appId;
    private final String secret;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeliLegalSearchClient(
            @Value("${lexai.deli.app-id:}") String appId,
            @Value("${lexai.deli.secret:}") String secret,
            @Value("${lexai.deli.timeout-ms:10000}") int timeoutMs,
            ObjectMapper objectMapper
    ) {
        this.appId = appId;
        this.secret = secret;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                .build();
    }

    public List<String> searchLaw(String keywords, boolean semantic) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        if (appId.isBlank() || secret.isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> payload = Map.of(
                    "pageNo", 1,
                    "pageSize", 5,
                    "sortField", "correlation",
                    "sortOrder", "desc",
                    "condition", Map.of(
                            "keywords", List.of(keywords),
                            "fieldName", semantic ? "semantic" : "title"
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                    .header("Content-Type", "application/json")
                    .header("appid", appId)
                    .header("secret", secret)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            return extractLawItems(response.body(), 5);
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private List<String> extractLawItems(String body, int limit) {
        try {
            JsonNode root = objectMapper.readTree(body);
            // 得理 queryListLaw 返回结构：{ success, code, body: { data: [ ... ] } }
            JsonNode dataNode = root.path("body").path("data");
            List<JsonNode> items = new ArrayList<>();
            if (dataNode.isArray()) {
                dataNode.forEach(items::add);
            } else {
                items = flattenArrayCandidates(root);
            }
            if (items.isEmpty()) {
                return List.of();
            }
            Set<String> dedup = new LinkedHashSet<>();
            for (JsonNode n : items) {
                String title = stripHtml(firstText(n,
                        "title", "lawName", "regulationName", "name"));
                String issuedNo = firstText(n, "issuedNo", "articleNo", "number");
                String publisher = firstText(n, "publisherName", "publisher", "issuer");
                String highlight = stripHtml(extractHighlight(n));
                String fallbackContent = stripHtml(firstText(n,
                        "content", "text", "summary"));
                String content = !highlight.isBlank() ? highlight : fallbackContent;

                String head = joinNonBlank(title, issuedNo);
                String tail = joinNonBlank(publisher, content);
                String line = joinNonBlank(head, tail);
                if (!line.isBlank()) {
                    dedup.add(truncate(line, 220));
                }
                if (dedup.size() >= limit) {
                    break;
                }
            }
            return new ArrayList<>(dedup);
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private static String extractHighlight(JsonNode n) {
        JsonNode hl = n.get("highlights");
        if (hl == null || !hl.isArray() || hl.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : hl) {
            String t = item.path("text").asText("");
            if (!t.isBlank()) {
                if (!sb.isEmpty()) sb.append(" / ");
                sb.append(t.trim());
            }
        }
        return sb.toString();
    }

    private static String stripHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String firstText(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && !v.isNull()) {
                String s = v.asText("");
                if (!s.isBlank()) return s;
            }
        }
        return "";
    }

    private static List<JsonNode> flattenArrayCandidates(JsonNode root) {
        List<JsonNode> out = new ArrayList<>();
        walk(root, out, 0);
        return out;
    }

    private static void walk(JsonNode node, List<JsonNode> out, int depth) {
        if (node == null || depth > 6) return;
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (child != null && child.isObject()) {
                    out.add(child);
                }
                walk(child, out, depth + 1);
            }
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> walk(e.getValue(), out, depth + 1));
        }
    }
}
