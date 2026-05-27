package de.itsec.api.controllers.v1;

import de.itsec.api.data.dto.response.PrivilegeDto;
import de.itsec.api.services.PrivilegeService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privileges")
@Secured({"ROLE_ADMIN"})
public class PrivilegeController {

  private PrivilegeService privilegeService;

  @GetMapping
  public ResponseEntity<List<PrivilegeDto>> getAllPrivileges() {
    return ResponseEntity.ok(
        privilegeService.getAllPrivileges().stream().map(PrivilegeDto::from).toList());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePrivilege(@PathVariable Long id) {
    privilegeService.deletePrivilege(id);
    return ResponseEntity.noContent().build();
  }

  @Autowired
  public PrivilegeController(PrivilegeService privilegeService) {
    this.privilegeService = privilegeService;
  }
}
