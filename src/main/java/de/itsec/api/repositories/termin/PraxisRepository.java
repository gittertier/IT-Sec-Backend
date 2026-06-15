package de.itsec.api.repositories.termin;

import de.itsec.api.data.termin.Praxis;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PraxisRepository extends JpaRepository<Praxis, UUID> {

  Optional<Praxis> findByName(String name);
}
