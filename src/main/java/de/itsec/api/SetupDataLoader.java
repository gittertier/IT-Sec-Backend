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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    List<Praxis> praxen = seedMainzPraxen();
    createStaffIfNotFound(praxen.get(0));
    alreadySetup = true;
  }

  private static final String[] DEMO_VACCINES = {
    "Comirnaty (BioNTech)", "Spikevax (Moderna)", "Nuvaxovid (Novavax)"
  };

  // Seeds real Mainz practices (2-4 per PLZ area, real addresses and coordinates)
  // so the map and the PLZ search show something sensible. Each newly created
  // praxis is filled with free slots inside createPraxis.
  private List<Praxis> seedMainzPraxen() {
    List<Praxis> praxen = new ArrayList<>();
    // 55116 - Altstadt / Mitte
    praxen.add(createPraxis("Impfzentrum Mainz-Mitte", "Grosse Bleiche", "46", "55116", 50.0007, 8.2697));
    praxen.add(createPraxis("Hausarztpraxis Schillerplatz", "Schillerstrasse", "10", "55116", 49.9986, 8.2705));
    praxen.add(createPraxis("Praxis am Markt", "Markt", "12", "55116", 50.0000, 8.2730));
    // 55118 - Neustadt
    praxen.add(createPraxis("Hausarztpraxis Mainz-Neustadt", "Wallaustrasse", "11", "55118", 50.0061, 8.2585));
    praxen.add(createPraxis("Gemeinschaftspraxis Goetheplatz", "Goethestrasse", "7", "55118", 50.0042, 8.2620));
    praxen.add(createPraxis("Praxis Kaiser-Wilhelm-Ring", "Kaiser-Wilhelm-Ring", "54", "55118", 50.0075, 8.2640));
    // 55120 - Mombach
    praxen.add(createPraxis("Gesundheitszentrum Mombach", "Hauptstrasse", "50", "55120", 50.0192, 8.2386));
    praxen.add(createPraxis("Hausarztpraxis Mombach-West", "Westring", "20", "55120", 50.0205, 8.2350));
    // 55124 - Gonsenheim
    praxen.add(createPraxis("Praxis Gonsenheim", "Breite Strasse", "33", "55124", 50.0095, 8.2110));
    praxen.add(createPraxis("Hausarztzentrum Gonsenheim", "Mainzer Strasse", "75", "55124", 50.0080, 8.2150));
    // 55127 - Lerchenberg / Drais
    praxen.add(createPraxis("Praxis am Lerchenberg", "Hindemithstrasse", "6", "55127", 49.9525, 8.2147));
    praxen.add(createPraxis("Hausarztpraxis Drais", "Draiser Strasse", "120", "55127", 49.9600, 8.2000));
    // 55131 - Oberstadt
    praxen.add(createPraxis("Universitaetsmedizin Mainz", "Langenbeckstrasse", "1", "55131", 49.9926, 8.2735));
    praxen.add(createPraxis("Praxis Oberstadt", "Hechtsheimer Strasse", "2", "55131", 49.9850, 8.2750));
    praxen.add(createPraxis("Hausarztpraxis Am Fort", "Am Fort Elisabeth", "5", "55131", 49.9880, 8.2800));
    return praxen;
  }

  private Praxis createPraxis(
      String name, String street, String houseNumber, String plz, double lat, double lon) {
    Optional<Praxis> existing = praxisRepository.findByName(name);
    if (existing.isPresent()) {
      return existing.get();
    }
    Praxis praxis = new Praxis();
    praxis.setName(name);
    praxis.setAddress(
        Address.builder()
            .street(street)
            .houseNumber(houseNumber)
            .city("Mainz")
            .areaCode(plz)
            .build());
    praxis.setLatitude(lat);
    praxis.setLongitude(lon);
    praxis = praxisRepository.save(praxis);
    // Seed slots only for a newly created praxis, so a persistent DB does not pile
    // up duplicate slots on every restart.
    seedSlots(praxis);
    return praxis;
  }

  // Free 30-minute slots for the coming week, weekdays 09:00-15:30. Slots that are
  // already in the past (earlier today) are skipped, so the "from today" search
  // always starts with bookable times. Vaccines rotate for some filter variety.
  private void seedSlots(Praxis praxis) {
    LocalDateTime now = LocalDateTime.now();
    int vaccineIndex = 0;
    for (int dayOffset = 0; dayOffset < 21; dayOffset++) {
      LocalDate day = LocalDate.now().plusDays(dayOffset);
      if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
        continue;
      }
      for (int slot = 0; slot < 14; slot++) {
        LocalDateTime start = LocalDateTime.of(day, LocalTime.of(9, 0)).plusMinutes(30L * slot);
        if (start.isBefore(now)) {
          continue;
        }
        Termin termin = new Termin();
        termin.setPraxis(praxis);
        termin.setStartTime(start);
        termin.setEndTime(start.plusMinutes(30));
        termin.setStatus(TerminStatus.FREE);
        termin.setVaccine(DEMO_VACCINES[vaccineIndex % DEMO_VACCINES.length]);
        terminRepository.save(termin);
        vaccineIndex++;
      }
    }
  }

  private void createStaffIfNotFound(Praxis praxis) {
    if (userRepository.findByUsername("staff@staff.com").isPresent()) {
      return;
    }
    Role staffRole = roleRepository.findByName("ROLE_STAFF");
    User user = new User();
    user.setPassword(passwordEncoder.encode("staff"));
    user.setUsername("staff@staff.com");
    user.setFirstName("staff");
    user.setLastName("staff");
    // Email pre-verified like the other seed accounts, so the demo only has to do
    // the TOTP step of onboarding (no SMTP needed for the seeded users).
    user.setEmailVerified(true);
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
    if (userRepository.findByUsername("user@user.com").isPresent()) {
      return;
    }
    Role adminRole = roleRepository.findByName("ROLE_USER");
    User user = new User();
    user.setPassword(passwordEncoder.encode("test"));
    user.setUsername("user@user.com");
    user.setFirstName("Test");
    user.setLastName("Nutzer");
    // Real Mainz address in PLZ 55116, where the seeded "Impfzentrum Mainz-Mitte"
    // sits, so the appointment search prefilled from this address finds slots.
    user.setAddress(
        Address.builder()
            .city("Mainz")
            .houseNumber("10")
            .street("Ludwigsstrasse")
            .areaCode("55116")
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
