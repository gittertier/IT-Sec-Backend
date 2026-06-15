package de.itsec.api.data.dto.response;

import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public view of an appointment slot. Deliberately omits the pseudonym so no identifying handle
 * leaves the service.
 */
public record TerminDto(
    UUID id,
    UUID praxisId,
    String praxisName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    TerminStatus status,
    String note) {

  public static TerminDto from(Termin termin) {
    return new TerminDto(
        termin.getId(),
        termin.getPraxis().getId(),
        termin.getPraxis().getName(),
        termin.getStartTime(),
        termin.getEndTime(),
        termin.getStatus(),
        termin.getNote());
  }
}
