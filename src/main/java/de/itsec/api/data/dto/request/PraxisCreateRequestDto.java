package de.itsec.api.data.dto.request;

import de.itsec.api.data.dto.response.AddressDto;
import de.itsec.api.data.dto.response.CoordinatesDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for creating a new {@link de.itsec.api.data.termin.Praxis} (admin only).
 *
 * <p>The address reuses {@link AddressDto} so praxen and users share one address shape. The PLZ is
 * the address postalCode (stored as areaCode). The PLZ format is checked by the frontend and by the
 * Address entity (areaCode length), so the request payload stays simple here.
 */
public record PraxisCreateRequestDto(
    @NotNull(message = "name cannot be null") @NotEmpty(message = "name cannot be empty")
        String name,
    @NotNull(message = "address cannot be null") @Valid AddressDto address,
    @NotNull(message = "coordinates cannot be null") CoordinatesDto coordinates) {}
