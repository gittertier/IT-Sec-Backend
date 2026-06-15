package de.itsec.api.repositories.termin;

import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminRepository extends JpaRepository<Termin, UUID> {

  /** Free, future slots of a praxis, ordered chronologically. */
  List<Termin> findByPraxisIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
      UUID praxisId, TerminStatus status, LocalDateTime after);

  /** Free, future slots across all praxen, ordered chronologically. */
  List<Termin> findByStatusAndStartTimeAfterOrderByStartTimeAsc(
      TerminStatus status, LocalDateTime after);

  /** All slots currently held by the given pseudonym. */
  List<Termin> findByPseudoUserIdOrderByStartTimeAsc(UUID pseudoUserId);

  /** Active (booked, future) slots held by the given pseudonym. */
  List<Termin> findByPseudoUserIdAndStatusAndStartTimeAfter(
      UUID pseudoUserId, TerminStatus status, LocalDateTime after);
}
