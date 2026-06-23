package de.itsec.api.services;

import de.itsec.api.data.termin.Praxis;
import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.repositories.termin.PraxisRepository;
import de.itsec.api.repositories.termin.TerminRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final PraxisRepository praxisRepository;
  private final PseudoMappingService pseudoMappingService;

  @Autowired
  public TerminService(
      TerminRepository terminRepository,
      PraxisRepository praxisRepository,
      PseudoMappingService pseudoMappingService) {
    this.terminRepository = terminRepository;
    this.praxisRepository = praxisRepository;
    this.pseudoMappingService = pseudoMappingService;
  }

  /**
   * Creates a new free slot at a praxis. Admin/staff operation; the slot starts as {@link
   * TerminStatus#FREE} with no booker.
   *
   * @param praxisId the praxis the slot belongs to
   * @param startTime slot start; must be in the future and before {@code endTime}
   * @param endTime slot end
   * @return the created slot
   */
  @Transactional
  public Termin createSlot(
      UUID praxisId, LocalDateTime startTime, LocalDateTime endTime, String vaccine) {
    Praxis praxis =
        praxisRepository
            .findById(praxisId)
            .orElseThrow(PublicExceptions.PraxisNotFoundException::new);

    LocalDateTime now = LocalDateTime.now();
    if (startTime == null
        || endTime == null
        || !startTime.isAfter(now)
        || !startTime.isBefore(endTime)) {
      throw new PublicExceptions.InvalidSlotTimeException();
    }

    Termin slot = new Termin();
    slot.setPraxis(praxis);
    slot.setStartTime(startTime);
    slot.setEndTime(endTime);
    slot.setStatus(TerminStatus.FREE);
    if (vaccine != null && !vaccine.isBlank()) {
      slot.setVaccine(vaccine);
    }
    return terminRepository.save(slot);
  }

  /**
   * Filters slots by praxis, postal code, status and start-time range. Every argument is optional;
   * {@code null} disables that criterion.
   */
  public Page<Termin> filter(
      UUID praxisId,
      String postalCode,
      TerminStatus status,
      LocalDateTime from,
      LocalDateTime to,
      Pageable pageable) {
    String pc = (postalCode != null && !postalCode.isBlank()) ? postalCode : null;
    return terminRepository.filter(praxisId, pc, status, from, to, pageable);
  }

  /**
   * Free, future slots a user can choose from. Each slot carries its praxis, so no separate praxis
   * lookup is needed.
   *
   * @param praxisId optional filter; when {@code null}, slots of all praxen are returned
   * @param plz optional postal-code filter on the slot's praxis; blank/{@code null} disables it
   * @param from earliest start time to return; lets callers look further into the future. {@code
   *     null} or a past value defaults to "now" so past slots are never returned
   */
  public Page<Termin> getFreeSlots(
      UUID praxisId, String plz, LocalDateTime from, Pageable pageable) {
    String postalCode = (plz != null && !plz.isBlank()) ? plz : null;
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime effectiveFrom = (from != null && from.isAfter(now)) ? from : now;
    return terminRepository.filter(
        praxisId, postalCode, TerminStatus.FREE, effectiveFrom, null, pageable);
  }

  /**
   * The user's own slots (any status when no filter is given) with optional praxis, status and
   * start-time range filters (e.g. "my booked
   * appointments at praxis X on day Y"). Always scoped to the caller's pseudonym, so no other user's
   * appointments can be reached.
   */
  public Page<Termin> getAppointmentsForUser(
      UUID userId,
      UUID praxisId,
      TerminStatus status,
      LocalDateTime from,
      LocalDateTime to,
      Pageable pageable) {
    UUID pseudoId = pseudoMappingService.getOrCreatePseudoIdFor(userId);
    return terminRepository.filterForPseudoUser(pseudoId, praxisId, status, from, to, pageable);
  }

  /**
   * Books a free slot for a user.
   *
   * @param slotId the slot to book
   * @param userId the real user id of the booking user
   * @return the booked slot
   */
  @Transactional
  public Termin book(UUID slotId, UUID userId) {
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
    terminRepository.save(slot);
  }
}
