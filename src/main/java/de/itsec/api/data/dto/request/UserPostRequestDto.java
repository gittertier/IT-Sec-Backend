package de.itsec.api.data.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UserPostRequestDto(
    @NotNull(message = "username cannot be null") @NotEmpty(message = "username cannot be empty")
        String username,
    @NotNull(message = "password cannot be null") @NotEmpty(message = "password cannot be empty")
        String password) {}
