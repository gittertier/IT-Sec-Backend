package de.itsec.api.repositories.authentication;

import de.itsec.api.data.authentication.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Role findByName(String name);
}
