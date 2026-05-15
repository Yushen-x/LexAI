package com.lexai.backend.application.dto.response;

import java.util.List;

public record RetrievalContext(
        List<String> laws,
        List<String> cases,
        List<String> knowledge
) {
}
