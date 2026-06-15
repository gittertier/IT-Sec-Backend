package de.itsec.api.services;

import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.repositories.termin.TerminRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Appointment management logic.
 *
 * <p>Bookings are stored against a pseudonym ({@link Termin#getPseudoUserId()}) resolved through
 * {@link PseudoMappingService}, never against the real user id, so the appointment table cannot be
 * traced back to a person on its own.
 */
@Service
public class TerminService {

  /** Maximum number of upcoming, booked slots a single user may hold at once. */
  static final int MAX_ACTIVE_BOOKINGS_PER_USER = 3;

  private final TerminRepository terminRepository;
  private final PseudoMappingService pseudoMappingService;

  @Autowired
  public TerminService(
      TerminRepository terminRepository, PseudoMappingService pseudoMappingService) {
    this.terminRepository = terminRepository;
    this.pseudoMappingService = pseudoMappingService;
  }

  /**
   * Free, future slots a user can choose from. Each slot carries its praxis, so no separate praxis
   * lookup is needed.
   *
   * @param praxisId optional filter; when {@code null}, slots of all praxen are returned
   */
  public List<Termin> getFreeSlots(UUID praxisId) {
    LocalDateTime now = LocalDateTime.now();
    if (praxisId == null) {
      return terminRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(
          TerminStatus.FREE, now);
    }
    return terminRepository.findByPraxisIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
        praxisId, TerminStatus.FREE, now);
  }

  /** All slots (any status) currently associated with the given user, via their pseudonym. */
  public List<Termin> getAppointmentsForUser(UUID userId) {
    UUID pseudoId = pseudoMappingService.getOrCreatePseudoIdFor(userId);
    return terminRepository.findByPseudoUserIdOrderByStartTimeAsc(pseudoId);
  }

  /**
   * Books a free slot for a user.
   *
   * @param slotId the slot to book
   * @param userId the real user id of the booking user
   * @param note optional free-text note, stored encrypted; may be {@code null}
   * @return the booked slot
   */
  @Transactional
  public Termin book(UUID slotId, UUID userId, String note) {
    Termin slot =
        terminRepository
            .findById(slotId)
            .orElseThrow(PublicExceptions.TerminNotFoundException::new);

    LocalDateTime now = LocalDateTime.now();
    if (slot.getStatus() != TerminStatus.FREE || slot.getStartTime().isBefore(now)) {
      throw new PublicExceptions.SlotNotBookableException();
    }

    UUID pseudoId = pseudoMappingService.getOrCreatePseudoIdFor(userId);

    int activeBookings =
        terminRepository
            .findByPseudoUserIdAndStatusAndStartTimeAfter(pseudoId, TerminStatus.BOOKED, now)
            .size();
    if (activeBookings >= MAX_ACTIVE_BOOKINGS_PER_USER) {
      throw new PublicExceptions.BookingLimitReachedException(MAX_ACTIVE_BOOKINGS_PER_USER);
    }

    slot.setStatus(TerminStatus.BOOKED);
    slot.setPseudoUserId(pseudoId);
    if (note != null && !note.isBlank()) {
      slot.setNote(note);
    }
    return terminRepository.save(slot);
  }

  /**
   * Cancels a slot the user currently holds, releasing it back to {@link TerminStatus#FREE}.
   *
   * @param slotId the booked slot
   * @param userId the real user id of the requesting user
   */
  @Transactional
  public void cancel(UUID slotId, UUID userId) {
    Termin slot =
        terminRepository
            .findById(slotId)
            .orElseThrow(PublicExceptions.TerminNotFoundException::new);

    UUID pseudoId = pseudoMappingService.getOrCreatePseudoIdFor(userId);
    if (slot.getStatus() != TerminStatus.BOOKED || !pseudoId.equals(slot.getPseudoUserId())) {
      throw new PublicExceptions.NotYourTerminException();
    }

    slot.setStatus(TerminStatus.FREE);
    slot.setPseudoUserId(null);
    slot.setNote(null);
    terminRepository.save(slot);
  }
}
