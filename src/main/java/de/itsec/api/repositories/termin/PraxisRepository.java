package de.itsec.api.repositories.termin;

import de.itsec.api.data.termin.Praxis;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PraxisRepository extends JpaRepository<Praxis, UUID> {

  Optional<Praxis> findByName(String name);

  /** Praxen with the given postal code (PLZ), paged. The PLZ lives in the address (areaCode). */
  Page<Praxis> findByAddress_AreaCode(String areaCode, Pageable pageable);

  /** Praxen whose name contains the given text, case-insensitive, paged. */
  Page<Praxis> findByNameContainingIgnoreCase(String name, Pageable pageable);

  /** Praxen matching both postal code and a case-insensitive name fragment, paged. */
  Page<Praxis> findByAddress_AreaCodeAndNameContainingIgnoreCase(
      String areaCode, String name, Pageable pageable);
}
