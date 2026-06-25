package de.itsec.api.data.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload for creating a new free {@link de.itsec.api.data.termin.Termin} slot at a praxis
 * (admin/staff only). The slot is created as {@link de.itsec.api.data.termin.TerminStatus#FREE}.
 *
 * <p>{@code vaccine} is optional and names the vaccine offered at the slot. {@code praxisId} is
 * optional too: a staff member can omit it and the slot goes to their assigned praxis; an admin
 * must provide it.
 */
public record TerminCreateRequestDto(
    UUID praxisId,
    @NotNull(message = "startTime cannot be null") @Future(message = "startTime must be in the future")
        LocalDateTime startTime,
    @NotNull(message = "endTime cannot be null") @Future(message = "endTime must be in the future")
        LocalDateTime endTime,
    String vaccine) {}
