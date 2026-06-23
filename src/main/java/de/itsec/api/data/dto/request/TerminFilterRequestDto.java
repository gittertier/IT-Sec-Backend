package de.itsec.api.data.dto.request;

import de.itsec.api.data.termin.TerminStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filter criteria for listing appointment slots. All fields are optional; a {@code null} field means
 * "do not filter on this". The response is a list of {@link
 * de.itsec.api.data.dto.response.TerminDto}.
 *
 * @param praxisId restrict to a single praxis
 * @param postalCode restrict to praxen with this German postal code (PLZ)
 * @param from earliest start time (inclusive)
 * @param to latest start time (inclusive)
 * @param status restrict to a single slot status (e.g. {@link TerminStatus#FREE})
 */
public record TerminFilterRequestDto(
    UUID praxisId, String postalCode, LocalDateTime from, LocalDateTime to, TerminStatus status) {}
