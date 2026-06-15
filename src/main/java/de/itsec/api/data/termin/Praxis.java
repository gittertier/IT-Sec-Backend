package de.itsec.api.data.termin;

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
}
