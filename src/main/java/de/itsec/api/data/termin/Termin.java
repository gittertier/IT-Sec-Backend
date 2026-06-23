package de.itsec.api.data.termin;

import de.itsec.api.crypto.StringCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * A single appointment slot offered by a {@link Praxis}.
 *
 * <p>A slot is created as {@link TerminStatus#FREE}. When a user books it, only the pseudonymous
 * {@code pseudoUserId} is stored — never the real user id. The link back to the actual user lives
 * exclusively in the encrypted pseudo-mapping store, so an appointment row cannot be traced to a
 * person from the database alone.
 */
@Data
@Entity
@Table(name = "termine")
public class Termin {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "praxis_id")
  private Praxis praxis;

  @Column(nullable = false)
  private LocalDateTime startTime;

  @Column(nullable = false)
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TerminStatus status = TerminStatus.FREE;

  /** Pseudonym of the booking user; {@code null} while the slot is free. Never the real user id. */
  @Column(name = "pseudo_user_id")
  private UUID pseudoUserId;

  /**
   * Which vaccine is administered at this slot, shown to the user so they can pick the right
   * appointment (e.g. for a follow-up dose). Free text, not personal data; {@code null} if unset.
   */
  private String vaccine;

  /** Optional free-text note, encrypted at rest. */
  @Convert(converter = StringCryptoConverter.class)
  private String note;
}
