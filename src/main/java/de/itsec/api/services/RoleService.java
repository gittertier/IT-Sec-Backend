package de.itsec.api.services;

import de.itsec.api.PermissionRoles;
import de.itsec.api.data.authentication.Role;
import de.itsec.api.repositories.authentication.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  private RoleRepository roleRepository;

  public Role getRole(PermissionRoles role) {
    return roleRepository.findByName(role.getName());
  }

  @Autowired
  public RoleService(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }
}
