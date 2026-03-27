package com.lexai.backend.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 本地知识库向量检索（无外部依赖版）：
 * - 从配置目录加载 .txt 文档
 * - 文档切分（chunk）
 * - 计算 TF-IDF 向量并持久化到本地索引文件
 * - 运行时按余弦相似度返回 Top-K
 */
@Component
public class LocalKnowledgeSearchClient {

    private final String knowledgePath;
    private final String indexPath;
    private final int chunkSize;
    private final int chunkOverlap;
    private final ObjectMapper objectMapper;
    private volatile KnowledgeIndex cachedIndex;

    public LocalKnowledgeSearchClient(
            @Value("${lexai.knowledge.path:}") String knowledgePath,
            @Value("${lexai.knowledge.index-path:}") String indexPath,
            @Value("${lexai.knowledge.chunk-size:320}") int chunkSize,
            @Value("${lexai.knowledge.chunk-overlap:60}") int chunkOverlap,
            ObjectMapper objectMapper
    ) {
        this.knowledgePath = knowledgePath;
        this.indexPath = indexPath;
        this.chunkSize = Math.max(120, chunkSize);
        this.chunkOverlap = Math.max(20, Math.min(chunkOverlap, this.chunkSize / 2));
        this.objectMapper = objectMapper;
    }

    public List<String> searchTopK(String query, int topK) {
        if (query == null || query.isBlank() || knowledgePath == null || knowledgePath.isBlank()) {
            return List.of();
        }
        Path base = Path.of(knowledgePath);
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            return List.of();
        }

        try {
            KnowledgeIndex index = loadOrBuildIndex(base);
            if (index == null || index.chunks().isEmpty()) return List.of();
            Map<String, Double> queryVec = toTfIdfVector(query, index.idf());
            if (queryVec.isEmpty()) return List.of();
            List<ScoredDoc> scored = new ArrayList<>();
            for (ChunkRecord chunk : index.chunks()) {
                double score = cosine(queryVec, chunk.vector());
                if (score <= 0) continue;
                String snippet = chunk.text().length() > 220 ? chunk.text().substring(0, 220) + "..." : chunk.text();
                scored.add(new ScoredDoc(chunk.source(), snippet.replaceAll("\\s+", " "), score));
            }

            return scored.stream()
                    .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
                    .limit(Math.max(1, topK))
                    .map(s -> "[source=%s score=%.2f] %s".formatted(s.source(), s.score(), s.snippet()))
                    .toList();
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private KnowledgeIndex loadOrBuildIndex(Path base) throws IOException {
        if (cachedIndex != null) return cachedIndex;
        Path idxFile = resolveIndexPath(base);
        if (Files.exists(idxFile)) {
            KnowledgeIndex idx = objectMapper.readValue(Files.readString(idxFile, StandardCharsets.UTF_8), KnowledgeIndex.class);
            cachedIndex = idx;
            return idx;
        }
        KnowledgeIndex built = buildIndex(base);
        Files.createDirectories(idxFile.getParent());
        Files.writeString(idxFile, objectMapper.writeValueAsString(built), StandardCharsets.UTF_8);
        cachedIndex = built;
        return built;
    }

    private Path resolveIndexPath(Path base) {
        if (indexPath != null && !indexPath.isBlank()) return Path.of(indexPath);
        return base.resolve(".vector-index.json");
    }

    private KnowledgeIndex buildIndex(Path base) throws IOException {
        List<Path> txtFiles = Files.walk(base)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                .toList();

        List<RawChunk> rawChunks = new ArrayList<>();
        for (Path file : txtFiles) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content == null || content.isBlank()) continue;
            List<String> chunks = chunkText(content, chunkSize, chunkOverlap);
            int i = 0;
            for (String chunk : chunks) {
                i++;
                rawChunks.add(new RawChunk(file.getFileName().toString() + "#" + i, chunk));
            }
        }

        if (rawChunks.isEmpty()) return new KnowledgeIndex(new HashMap<>(), List.of());
        Map<String, Double> idf = computeIdf(rawChunks);
        List<ChunkRecord> chunks = new ArrayList<>();
        for (RawChunk c : rawChunks) {
            Map<String, Double> vec = toTfIdfVector(c.text(), idf);
            if (vec.isEmpty()) continue;
            chunks.add(new ChunkRecord(c.source(), c.text(), vec));
        }
        return new KnowledgeIndex(idf, chunks);
    }

    private static List<String> chunkText(String content, int size, int overlap) {
        String normalized = content.replace("\r", "").trim();
        if (normalized.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + size);
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) out.add(chunk);
            if (end >= normalized.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return out;
    }

    private static Map<String, Double> computeIdf(List<RawChunk> rawChunks) {
        Map<String, Integer> df = new HashMap<>();
        int n = rawChunks.size();
        for (RawChunk c : rawChunks) {
            Set<String> uniq = new HashSet<>(tokenize(c.text()));
            for (String t : uniq) df.put(t, df.getOrDefault(t, 0) + 1);
        }
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            // smooth idf
            double v = Math.log((n + 1.0) / (e.getValue() + 1.0)) + 1.0;
            idf.put(e.getKey(), v);
        }
        return idf;
    }

    private static Map<String, Double> toTfIdfVector(String text, Map<String, Double> idf) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) return Map.of();
        Map<String, Integer> tfCount = new HashMap<>();
        for (String t : tokens) tfCount.put(t, tfCount.getOrDefault(t, 0) + 1);
        Map<String, Double> vec = new HashMap<>();
        int total = tokens.size();
        for (Map.Entry<String, Integer> e : tfCount.entrySet()) {
            double tf = e.getValue() * 1.0 / total;
            double idfV = idf.getOrDefault(e.getKey(), 1.0);
            vec.put(e.getKey(), tf * idfV);
        }
        return normalize(vec);
    }

    private static Map<String, Double> normalize(Map<String, Double> vec) {
        double norm = 0.0;
        for (double v : vec.values()) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm <= 1e-12) return Map.of();
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<String, Double> e : vec.entrySet()) out.put(e.getKey(), e.getValue() / norm);
        return out;
    }

    private static double cosine(Map<String, Double> q, Map<String, Double> d) {
        Map<String, Double> small = q.size() <= d.size() ? q : d;
        Map<String, Double> large = small == q ? d : q;
        double dot = 0.0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double v = large.get(e.getKey());
            if (v != null) dot += e.getValue() * v;
        }
        return dot;
    }

    /**
     * 中文 + 英文数字分词：
     * - 中文：按单字切分（简化版，便于无外部依赖运行）
     * - 英文/数字：按连续字母数字切分
     */
    private static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (isAsciiWord(ch)) {
                latin.append(ch);
                continue;
            }
            if (latin.length() > 1) {
                out.add(latin.toString());
            }
            latin.setLength(0);
            if (isChinese(ch)) {
                out.add(String.valueOf(ch));
            }
        }
        if (latin.length() > 1) out.add(latin.toString());
        return out;
    }

    private static boolean isAsciiWord(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }

    private static boolean isChinese(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private record ScoredDoc(String source, String snippet, double score) {
    }

    private record RawChunk(String source, String text) {
    }

    public record ChunkRecord(String source, String text, Map<String, Double> vector) {
    }

    public record KnowledgeIndex(Map<String, Double> idf, List<ChunkRecord> chunks) {
    }
}
