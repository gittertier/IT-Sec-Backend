package de.itsec.api.services;

import de.itsec.api.data.authentication.Privilege;
import de.itsec.api.repositories.authentication.PrivilegeRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PrivilegeService {

  private PrivilegeRepository privilegeRepository;

  public List<Privilege> getAllPrivileges() {
    return privilegeRepository.findAll();
  }

  public void deletePrivilege(Long id) {
    privilegeRepository.deleteById(id);
  }

  public PrivilegeService(PrivilegeRepository privilegeRepository) {
    this.privilegeRepository = privilegeRepository;
  }
}
