package de.itsec.api.repositories.termin;

import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminRepository extends JpaRepository<Termin, UUID> {

  /**
   * Flexible slot filter. Every parameter is optional; passing {@code null} disables that criterion.
   * Filters by praxis, the praxis' postal code, status and a start-time range, ordered
   * chronologically.
   */
  @Query(
      "SELECT t FROM Termin t WHERE "
          + "(:praxisId IS NULL OR t.praxis.id = :praxisId) AND "
          + "(:postalCode IS NULL OR t.praxis.postalCode = :postalCode) AND "
          + "(:status IS NULL OR t.status = :status) AND "
          + "(:from IS NULL OR t.startTime >= :from) AND "
          + "(:to IS NULL OR t.startTime <= :to) "
          + "ORDER BY t.startTime ASC")
  List<Termin> filter(
      @Param("praxisId") UUID praxisId,
      @Param("postalCode") String postalCode,
      @Param("status") TerminStatus status,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

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

  /**
   * A single user's own slots (scoped by pseudonym) with optional praxis, status and start-time
   * range filters. {@code null} disables that criterion.
   */
  @Query(
      "SELECT t FROM Termin t WHERE t.pseudoUserId = :pseudoUserId AND "
          + "(:praxisId IS NULL OR t.praxis.id = :praxisId) AND "
          + "(:status IS NULL OR t.status = :status) AND "
          + "(:from IS NULL OR t.startTime >= :from) AND "
          + "(:to IS NULL OR t.startTime <= :to) "
          + "ORDER BY t.startTime ASC")
  List<Termin> filterForPseudoUser(
      @Param("pseudoUserId") UUID pseudoUserId,
      @Param("praxisId") UUID praxisId,
      @Param("status") TerminStatus status,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);
}
