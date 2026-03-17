package com.lexai.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ConsultationRequest(
        @NotBlank(message = "question must not be blank")
        String question,
        List<String> facts
) {
}

