package de.itsec.api.repositories.termin;

import de.itsec.api.data.termin.StaffAssignment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {

  /** The assignment for a staff member, looked up by their pseudonym. */
  Optional<StaffAssignment> findByPseudoUserId(UUID pseudoUserId);
}
