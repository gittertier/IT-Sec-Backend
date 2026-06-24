package de.itsec.api.data.termin;

import de.itsec.api.data.Address;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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

  /**
   * Structured address, reusing the same Address entity that User uses. The postal code (PLZ) lives
   * in Address.areaCode, which is the single value we filter praxen and slots by, so there is no
   * separate postalCode column anymore. Required (optional = false): a praxis always has an address.
   * This also keeps the PLZ filter queries safe, because they reach the PLZ through this address.
   */
  @ManyToOne(optional = false, cascade = CascadeType.ALL)
  private Address address;
}
