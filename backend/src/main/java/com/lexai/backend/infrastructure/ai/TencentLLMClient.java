package com.lexai.backend.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 腾讯大模型 HTTP 客户端（endpoint 可配置）。
 *
 * 注意：
 * - 不同腾讯模型接入方式返回结构可能不同，此处只做通用 best-effort 解析。
 * - 若未配置 endpoint / api-key 或调用失败，返回 null，由上层网关做兜底。
 */
@Component
public class TencentLLMClient {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TencentLLMClient(
            @Value("${lexai.tencent.llm.endpoint:}") String endpoint,
            @Value("${lexai.tencent.llm.api-key:}") String apiKey,
            @Value("${lexai.tencent.llm.model:}") String model,
            @Value("${lexai.tencent.llm.timeout-ms:20000}") int timeoutMs,
            ObjectMapper objectMapper
    ) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", model == null ? "" : model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                            Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)
                    ),
                    "temperature", 0.2
            );

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                    .header("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return extractText(response.body());
        } catch (Exception ignore) {
            return null;
        }
    }

    private String extractText(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            // 常见 OpenAI 兼容结构: choices[0].message.content
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && !content.isNull() && !content.asText("").isBlank()) {
                return content.asText();
            }

            // 兼容其他结构: data.content / output.text / answer
            String[] candidates = {"answer", "content", "text"};
            for (String key : candidates) {
                JsonNode node = root.get(key);
                if (node != null && !node.isNull()) {
                    String v = node.asText("");
                    if (!v.isBlank()) return v;
                }
            }
            JsonNode outputText = root.path("output").path("text");
            if (!outputText.isMissingNode()) {
                String v = outputText.asText("");
                if (!v.isBlank()) return v;
            }
            JsonNode dataContent = root.path("data").path("content");
            if (!dataContent.isMissingNode()) {
                String v = dataContent.asText("");
                if (!v.isBlank()) return v;
            }
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }
}
