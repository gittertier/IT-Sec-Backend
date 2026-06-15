package de.itsec.api.data.dto.request;

/** Optional payload when booking a slot. {@code note} is stored encrypted at rest. */
public record TerminBookingRequestDto(String note) {}
