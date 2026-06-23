package de.itsec.api.repositories.termin;

import de.itsec.api.data.termin.Praxis;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PraxisRepository extends JpaRepository<Praxis, UUID> {

  Optional<Praxis> findByName(String name);

  /** All praxen with the given postal code (PLZ). */
  List<Praxis> findByPostalCode(String postalCode);

  /** All praxen whose name contains the given text, case-insensitive. */
  List<Praxis> findByNameContainingIgnoreCase(String name);

  /** All praxen matching both postal code and a case-insensitive name fragment. */
  List<Praxis> findByPostalCodeAndNameContainingIgnoreCase(String postalCode, String name);
}
