package com.penguinlist.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateTodoRequest(
        @NotBlank(message = "title is required")
        String title,
        List<String> descriptions
) {
}
