package de.itsec.api.data.dto.response;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Operator (staff) view of a slot. Same shape as {@link TerminDto} plus the booker for booked slots.
 * The booker is de-pseudonymized on purpose: a staff member is authorized to see who booked a slot
 * at their OWN praxis (to verify the appointment on site). It is only ever filled for staff, scoped
 * to their praxis, so the pseudonymity of bookings towards everyone else stays intact.
 */
public record StaffTerminDto(
    UUID id,
    UUID praxisId,
    String praxisName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    TerminStatus status,
    String vaccine,
    Booker booker) {

  public static StaffTerminDto from(Termin termin, Booker booker) {
    return new StaffTerminDto(
        termin.getId(),
        termin.getPraxis().getId(),
        termin.getPraxis().getName(),
        termin.getStartTime(),
        termin.getEndTime(),
        termin.getStatus(),
        termin.getVaccine(),
        booker);
  }

  public record Booker(String firstName, String lastName, String email, AddressDto address) {
    public static Booker from(User user) {
      return new Booker(
          user.getFirstName(),
          user.getLastName(),
          user.getUsername(),
          AddressDto.from(user.getAddress()));
    }
  }
}
