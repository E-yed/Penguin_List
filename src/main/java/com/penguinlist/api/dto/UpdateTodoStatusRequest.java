package com.penguinlist.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTodoStatusRequest(@NotNull Boolean done) {
}
