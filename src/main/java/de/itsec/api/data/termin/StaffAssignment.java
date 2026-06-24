package de.itsec.api.data.termin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

/**
 * Assigns a staff member to the {@link Praxis} they are allowed to manage.
 *
 * <p>Like {@link Termin}, the member is referenced only by their pseudonym ({@code pseudoUserId}),
 * never by their real user id — the link back to the person lives exclusively in the encrypted
 * pseudo-mapping store. The praxis itself is a plain foreign key, mirroring {@link Termin#getPraxis()}.
 */
@Data
@Entity
@Table(name = "praxis_staff")
public class StaffAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Pseudonym of the staff member; never the real user id. Unique — one praxis per member. */
  @Column(name = "pseudo_user_id", nullable = false, unique = true)
  private UUID pseudoUserId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "praxis_id")
  private Praxis praxis;
}
