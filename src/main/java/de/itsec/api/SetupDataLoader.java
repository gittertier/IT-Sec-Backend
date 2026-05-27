package de.itsec.api;

import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.itsec.api.data.authentication.Privilege;
import de.itsec.api.data.authentication.Role;
import de.itsec.api.data.authentication.User;
import de.itsec.api.repositories.authentication.PrivilegeRepository;
import de.itsec.api.repositories.authentication.RoleRepository;
import de.itsec.api.repositories.authentication.UserRepository;
import de.itsec.api.utils.AnnotationScanner;

@Component
public class SetupDataLoader
    implements ApplicationListener<ContextRefreshedEvent> {

  private UserRepository userRepository;

  private RoleRepository roleRepository;

  private PrivilegeRepository privilegeRepository;

  PasswordEncoder passwordEncoder;

  boolean alreadySetup = false;

  @Override
  @Transactional
  public void onApplicationEvent(ContextRefreshedEvent event) {

    if (alreadySetup) {
      return;
    }

    try {
      List<String> privilegeNames = AnnotationScanner.scan();
      privilegeNames.stream()
          .filter(privilegeName -> !privilegeName.startsWith("ROLE_"))
          .forEach(this::createPrivilegeIfNotFound);
    } catch (Exception e) {
      System.err.println("failed to load annotations/privileges" +
                         e.getMessage());
      e.printStackTrace();
    }

    Privilege readPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
    Privilege writePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");

    List<Privilege> adminPrivileges =
        Arrays.asList(readPrivilege, writePrivilege);
    createRoleIfNotFound("ROLE_ADMIN", adminPrivileges);
    createRoleIfNotFound("ROLE_STAFF", Arrays.asList(readPrivilege));
    createRoleIfNotFound("ROLE_TEST", Arrays.asList());
    createRoleIfNotFound("ROLE_USER", Arrays.asList(readPrivilege));

    // TODO: change!!!!
    createAdminIfNotFound();
    createUserIfNotFound();
    alreadySetup = true;
  }

  private void createAdminIfNotFound() {
    if (userRepository.findByUsername("admin").isPresent()) {
      return;
    }
    Role adminRole = roleRepository.findByName("ROLE_ADMIN");
    User user = new User();
    user.setPassword(passwordEncoder.encode("admin"));
    user.setUsername("admin");
    user.setEmail("admin@admin.com");
    user.setRoles(Arrays.asList(adminRole));
    user.setEnabled(true);
    userRepository.save(user);
  }

  private void createUserIfNotFound() {
    if (userRepository.findByUsername("user").isPresent()) {
      return;
    }
    Role adminRole = roleRepository.findByName("ROLE_USER");
    User user = new User();
    user.setPassword(passwordEncoder.encode("test"));
    user.setUsername("user");
    user.setEmail("user@user.com");
    user.setRoles(Arrays.asList(adminRole));
    user.setEnabled(true);
    userRepository.save(user);
  }

  @Transactional
  Privilege createPrivilegeIfNotFound(String name) {

    Privilege privilege = privilegeRepository.findByName(name);
    if (privilege == null) {
      privilege = new Privilege(name);
      privilegeRepository.save(privilege);
    }
    return privilege;
  }

  @Transactional
  Role createRoleIfNotFound(String name, Collection<Privilege> privileges) {

    Role role = roleRepository.findByName(name);
    if (role == null) {
      role = new Role(name);
      role.setPrivileges(privileges);
      roleRepository.save(role);
    }
    return role;
  }

  @Autowired
  public SetupDataLoader(UserRepository userRepository,
                         RoleRepository roleRepository,
                         PrivilegeRepository privilegeRepository,
                         PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.privilegeRepository = privilegeRepository;
    this.passwordEncoder = passwordEncoder;
  }
}
