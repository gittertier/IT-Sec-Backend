package de.itsec.api.data.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Payload for creating a new {@link de.itsec.api.data.termin.Praxis} (admin/staff only). */
public record PraxisCreateRequestDto(
    @NotNull(message = "name cannot be null") @NotEmpty(message = "name cannot be empty")
        String name,
    @NotNull(message = "address cannot be null") @NotEmpty(message = "address cannot be empty")
        String address,
    @NotNull(message = "postalCode cannot be null")
        @Pattern(regexp = "\\d{5}", message = "postalCode must be a 5-digit German PLZ")
        String postalCode) {}
