package de.itsec.api;

import de.itsec.api.data.Address;
import de.itsec.api.data.authentication.Privilege;
import de.itsec.api.data.authentication.Role;
import de.itsec.api.data.authentication.User;
import de.itsec.api.data.termin.Praxis;
import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import de.itsec.api.repositories.authentication.PrivilegeRepository;
import de.itsec.api.repositories.authentication.RoleRepository;
import de.itsec.api.repositories.authentication.UserRepository;
import de.itsec.api.repositories.termin.PraxisRepository;
import de.itsec.api.repositories.termin.TerminRepository;
import de.itsec.api.services.StaffPraxisService;
import de.itsec.api.utils.AnnotationScanner;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

  private UserRepository userRepository;

  private RoleRepository roleRepository;

  private PrivilegeRepository privilegeRepository;

  private PraxisRepository praxisRepository;

  private TerminRepository terminRepository;

  private StaffPraxisService staffPraxisService;

  PasswordEncoder passwordEncoder;

  boolean alreadySetup = false;

  private final String env;
  private final String adminUsername;
  private final String adminPassword;

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
      System.err.println("failed to load annotations/privileges" + e.getMessage());
      e.printStackTrace();
    }

    Privilege readPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
    Privilege writePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");

    List<Privilege> adminPrivileges = Arrays.asList(readPrivilege, writePrivilege);
    createRoleIfNotFound("ROLE_ADMIN", adminPrivileges);
    createRoleIfNotFound("ROLE_STAFF", Arrays.asList(readPrivilege));
    createRoleIfNotFound("ROLE_TEST", Arrays.asList());
    createRoleIfNotFound("ROLE_USER", Arrays.asList(readPrivilege));

    if (this.env.equals("prod")) {
      createAdminIfNotFound(this.adminUsername, this.adminPassword);
      alreadySetup = true;
      return;
    }

    // TODO: change!!!!
    createAdminIfNotFound();
    createUserIfNotFound();
    Praxis demoPraxis = createDemoPraxisIfNotFound();
    createStaffIfNotFound(demoPraxis);
    alreadySetup = true;
  }

  private Praxis createDemoPraxisIfNotFound() {
    Optional<Praxis> existing = praxisRepository.findByName("Demo Praxis");
    if (existing.isPresent()) {
      return existing.get();
    }
    Praxis praxis = new Praxis();
    praxis.setName("Demo Praxis");
    praxis.setAddress("Musterstrasse 1, 12345 Musterstadt");
    praxis.setPostalCode("12345");
    praxis = praxisRepository.save(praxis);

    // Seed a handful of free 30-minute slots for the next business day.
    LocalDateTime slotStart = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0));
    for (int i = 0; i < 8; i++) {
      Termin termin = new Termin();
      termin.setPraxis(praxis);
      termin.setStartTime(slotStart);
      termin.setEndTime(slotStart.plusMinutes(30));
      termin.setStatus(TerminStatus.FREE);
      terminRepository.save(termin);
      slotStart = slotStart.plusMinutes(30);
    }
    return praxis;
  }

  private void createStaffIfNotFound(Praxis praxis) {
    if (userRepository.findByUsername("staff").isPresent()) {
      return;
    }
    Role staffRole = roleRepository.findByName("ROLE_STAFF");
    User user = new User();
    user.setPassword(passwordEncoder.encode("staff"));
    user.setUsername("staff@staff.com");
    user.setFirstName("staff");
    user.setLastName("staff");
    user.setRoles(Arrays.asList(staffRole));
    User saved = userRepository.save(user);

    // Bind the staff member to their praxis pseudonymously, just like an appointment booking.
    staffPraxisService.assign(saved.getId(), praxis);
  }

  private void createAdminIfNotFound() {
    createAdminIfNotFound("admin@admin.com", "admin");
  }

  private void createAdminIfNotFound(String username, String password) {
    if (userRepository.findByUsername(username).isPresent()) {
      return;
    }
    Role adminRole = roleRepository.findByName("ROLE_ADMIN");
    User user = new User();
    user.setPassword(passwordEncoder.encode(password));
    user.setUsername(username);
    user.setFirstName("admin");
    user.setLastName("admin");
    user.setAddress(
        Address.builder()
            .city("admin city")
            .houseNumber("12")
            .street("admin street")
            .areaCode("99999")
            .build());
    user.setRoles(Arrays.asList(adminRole));
    user.setEmailVerified(true);
    userRepository.save(user);
  }

  private void createUserIfNotFound() {
    if (userRepository.findByUsername("user").isPresent()) {
      return;
    }
    Role adminRole = roleRepository.findByName("ROLE_USER");
    User user = new User();
    user.setPassword(passwordEncoder.encode("test"));
    user.setUsername("user@user.com");
    user.setAddress(
        Address.builder()
            .city("user city")
            .houseNumber("12")
            .street("user street")
            .areaCode("99999")
            .build());
    user.setRoles(Arrays.asList(adminRole));
    user.setEmailVerified(true);
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
  public SetupDataLoader(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PrivilegeRepository privilegeRepository,
      PraxisRepository praxisRepository,
      TerminRepository terminRepository,
      StaffPraxisService staffPraxisService,
      PasswordEncoder passwordEncoder,
      @Value("${prod.admin-password}") String adminPassword,
      @Value("${prod.admin-username}") String adminUsername,
      @Value("${app-env.mode}") String env) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.privilegeRepository = privilegeRepository;
    this.praxisRepository = praxisRepository;
    this.terminRepository = terminRepository;
    this.staffPraxisService = staffPraxisService;
    this.passwordEncoder = passwordEncoder;
    this.adminPassword = adminPassword;
    this.adminUsername = adminUsername;
    this.env = env;
  }
}
