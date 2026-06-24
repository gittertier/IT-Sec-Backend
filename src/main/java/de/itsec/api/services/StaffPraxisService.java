package de.itsec.api.services;

import de.itsec.api.data.termin.Praxis;
import de.itsec.api.data.termin.StaffAssignment;
import de.itsec.api.repositories.termin.StaffAssignmentRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves which {@link Praxis} a staff member may act on.
 *
 * <p>The association is stored pseudonymously, exactly like appointment bookings: the member is
 * referenced by the pseudonym from {@link PseudoMappingService}, never by their real user id, so the
 * {@code praxis_staff} table cannot be traced back to a person on its own.
 */
@Service
public class StaffPraxisService {

  private final StaffAssignmentRepository repository;
  private final PseudoMappingService pseudoMappingService;

  @Autowired
  public StaffPraxisService(
      StaffAssignmentRepository repository, PseudoMappingService pseudoMappingService) {
    this.repository = repository;
    this.pseudoMappingService = pseudoMappingService;
  }

  /**
   * Assigns (or reassigns) a staff member to a praxis, keyed by the member's pseudonym.
   *
   * @param userId the real user id of the staff member
   * @param praxis the praxis the member may manage
   */
  @Transactional
  public void assign(UUID userId, Praxis praxis) {
    UUID pseudoId = pseudoMappingService.getOrCreatePseudoIdFor(userId);
    StaffAssignment assignment =
        repository.findByPseudoUserId(pseudoId).orElseGet(StaffAssignment::new);
    assignment.setPseudoUserId(pseudoId);
    assignment.setPraxis(praxis);
    repository.save(assignment);
  }

  /**
   * The id of the praxis a staff member is assigned to, if any.
   *
   * @param userId the real user id of the staff member
   * @return the assigned praxis id, or empty if the member has no assignment
   */
  public Optional<UUID> praxisIdFor(UUID userId) {
    return pseudoMappingService
        .pseudoIdFor(userId)
        .flatMap(repository::findByPseudoUserId)
        .map(assignment -> assignment.getPraxis().getId());
  }
}
