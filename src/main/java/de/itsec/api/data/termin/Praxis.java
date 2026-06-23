package de.itsec.api.data.termin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

/**
 * A medical practice (Praxis) that offers appointment slots.
 *
 * <p>Praxis entities are managed internally (seeded / data layer) and are intentionally not exposed
 * through any REST endpoint.
 */
@Data
@Entity
@Table(name = "praxen")
public class Praxis {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;

  private String address;

  /** German postal code (PLZ), kept as a separate column so praxen can be filtered by it. */
  @Column(name = "postal_code")
  private String postalCode;
}
