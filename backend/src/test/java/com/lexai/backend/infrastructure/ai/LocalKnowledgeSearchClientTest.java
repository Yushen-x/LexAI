package com.lexai.backend.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link LocalKnowledgeSearchClient} 检索测试：用临时知识库目录验证倒排候选筛选后的检索
 * 结果正确性、缓存幂等性，以及无关查询不返回噪声。
 */
class LocalKnowledgeSearchClientTest {

    private LocalKnowledgeSearchClient newClient(Path knowledgeDir) {
        return new LocalKnowledgeSearchClient(
                knowledgeDir.toString(),
                knowledgeDir.resolve(".vector-index.json").toString(),
                320,
                60,
                new ObjectMapper());
    }

    private void writeDoc(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("检索命中词项最相关的文档作为 Top-1")
    void searchTopK_returnsMostRelevantDocument(@TempDir Path dir) throws IOException {
        writeDoc(dir, "lease.txt", "房屋租赁合同应当载明租金数额和租赁期限以及押金退还方式");
        writeDoc(dir, "labor.txt", "劳动合同应当载明劳动报酬工作时间和社会保险等事项");
        LocalKnowledgeSearchClient client = newClient(dir);

        List<String> hits = client.searchTopK("租金 押金 租赁期限", 3);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0)).contains("lease.txt");
    }

    @Test
    @DisplayName("topK 限制返回条数")
    void searchTopK_respectsLimit(@TempDir Path dir) throws IOException {
        writeDoc(dir, "a.txt", "合同审查要点包括主体资格和履行能力");
        writeDoc(dir, "b.txt", "合同审查要点还包括违约责任和争议解决");
        writeDoc(dir, "c.txt", "合同审查要点也包括知识产权归属");
        LocalKnowledgeSearchClient client = newClient(dir);

        assertThat(client.searchTopK("合同审查要点", 1)).hasSize(1);
        assertThat(client.searchTopK("合同审查要点", 2)).hasSize(2);
    }

    @Test
    @DisplayName("无共享词项的查询返回空，不产生噪声结果")
    void searchTopK_unrelatedQueryReturnsEmpty(@TempDir Path dir) throws IOException {
        writeDoc(dir, "lease.txt", "房屋租赁合同应当载明租金和租赁期限");
        LocalKnowledgeSearchClient client = newClient(dir);

        assertThat(client.searchTopK("zzzqueryzzz", 3)).isEmpty();
    }

    @Test
    @DisplayName("空/空白查询直接返回空")
    void searchTopK_blankQueryReturnsEmpty(@TempDir Path dir) throws IOException {
        writeDoc(dir, "lease.txt", "房屋租赁合同");
        LocalKnowledgeSearchClient client = newClient(dir);

        assertThat(client.searchTopK("", 3)).isEmpty();
        assertThat(client.searchTopK("   ", 3)).isEmpty();
        assertThat(client.searchTopK(null, 3)).isEmpty();
    }

    @Test
    @DisplayName("相同查询二次调用走缓存，结果与首次完全一致")
    void searchTopK_isCacheConsistent(@TempDir Path dir) throws IOException {
        writeDoc(dir, "lease.txt", "房屋租赁合同应当载明租金和租赁期限");
        writeDoc(dir, "labor.txt", "劳动合同载明劳动报酬");
        LocalKnowledgeSearchClient client = newClient(dir);

        List<String> first = client.searchTopK("租金 租赁期限", 3);
        List<String> second = client.searchTopK("租金 租赁期限", 3);

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("知识库目录不存在时安全返回空")
    void searchTopK_missingDirectoryReturnsEmpty(@TempDir Path dir) {
        LocalKnowledgeSearchClient client = newClient(dir.resolve("not-exist"));
        assertThat(client.searchTopK("租金", 3)).isEmpty();
    }
}
