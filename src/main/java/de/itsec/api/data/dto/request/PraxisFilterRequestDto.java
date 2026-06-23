package de.itsec.api.data.dto.request;

/**
 * Filter criteria for listing medical practices (Praxen). All fields are optional; a {@code null}
 * field means "do not filter on this". The response is a list of {@link
 * de.itsec.api.data.dto.response.PraxisDto}.
 *
 * @param postalCode restrict to praxen with this German postal code (PLZ)
 * @param name restrict to praxen whose name contains this text (case-insensitive)
 */
public record PraxisFilterRequestDto(String postalCode, String name) {}
