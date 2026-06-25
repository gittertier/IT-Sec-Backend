package de.itsec.api.repositories.authentication;

import de.itsec.api.data.authentication.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// User.id is a UUID, so the repository key must be UUID (was Long, which never
// matched a real row and broke the admin lookup/delete by id).
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByUsername(String username);
}
