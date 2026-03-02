package com.penguinlist.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDescriptionStatusRequest(@NotNull Boolean done) {
}
