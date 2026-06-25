package de.itsec.api.services;

import de.itsec.api.PermissionRoles;
import de.itsec.api.controllers.v1.UserController;
import de.itsec.api.data.Address;
import de.itsec.api.data.authentication.Role;
import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.UserPostRequestDto;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.repositories.authentication.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** UserService */
@Slf4j
@Service
public class UserService {

  private static final String PASSWORD_REGEX =
      "^(?=.*[a-z])" // at least one lowercase letter
          + "(?=.*[A-Z])" // at least one uppercase letter
          + "(?=.*\\d)" // at least one digit
          + "(?=.*[^A-Za-z\\d])" // at least one special character (any non-alphanumeric)
          + ".{12,}$"; // at least 12 characters (matches the client-side rule)

  private static final String CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

  UserRepository userRepository;

  RoleService roleService;

  PasswordEncoder passwordEncoder;

  EmailService emailService;

  /**
   * Updates the password for a given user if the current password is correct.
   *
   * @param username The username of the user.
   * @param currentPassword The current password of the user.
   * @param newPassword The new password to be set.
   * @throws Exception if the current password is incorrect or the user is not found.
   */
  public void updatePassword(String username, String currentPassword, String newPassword) {
    User user = verifyPassword(username, currentPassword);
    isPasswordStrong(newPassword);
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  /**
   * Checks if a user exists by username.
   *
   * @param username The username to check.
   * @return true if the user exists, false otherwise.
   */
  public boolean existsByUsername(String username) {
    return userRepository.findByUsername(username).isPresent();
  }

  public User getUserById(UUID id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new PublicExceptions.IllegalArgumentsException("User not found"));
  }

  public User patchUser(String username, UserController.UserPatchDto patch) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

    // username (= login identity) is intentionally NOT patchable here: changing it
    // goes through changeEmail with re-auth, uniqueness check and session reset.
    if (patch.firstName() != null) {
      user.setFirstName(patch.firstName());
    }
    if (patch.lastName() != null) {
      user.setLastName(patch.lastName());
    }

    if (patch.address() != null) {
      Address addr = user.getAddress();
      UserController.AddressPatchDto a = patch.address();
      if (addr == null) {
        addr = new Address();
      }
      if (a.street() != null) {
        addr.setStreet(a.street());
      }
      if (a.city() != null) {
        addr.setCity(a.city());
      }
      if (a.postalCode() != null) {
        addr.setAreaCode(a.postalCode());
      }
      if (a.houseNumber() != null) {
        addr.setHouseNumber(a.houseNumber());
      }

      user.setAddress(addr);
    }

    userRepository.save(user);
    return user;
  }

  /**
   * Looks up a user by username.
   *
   * @param username the username
   * @return the matching user
   * @throws UsernameNotFoundException if no such user exists
   */
  public User getUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  /**
   * Saves a user to the repository.
   *
   * @param user The user to save.
   */
  public void saveUser(User user) {
    userRepository.save(user);
  }

  /**
   * Deletes a user from the repository.
   *
   * @param id The id of the user to delete.
   */
  public void deleteUser(UUID id) {
    userRepository.deleteById(id);
  }

  /** Gets all users from the repository. */
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  /** Looks up a user by id without throwing (used to resolve a booker for staff). */
  public Optional<User> findById(UUID id) {
    return userRepository.findById(id);
  }

  public User createUser(UserPostRequestDto userDto) {

    if (this.userRepository.findByUsername(userDto.username()).isPresent()) {
      throw new PublicExceptions.UsernameAlreadyExistsException();
    }

    isPasswordStrong(userDto.password());

    Role userRole = this.roleService.getRole(PermissionRoles.USER);

    User user = new User();
    user.setPassword(passwordEncoder.encode(userDto.password()));
    user.setUsername(userDto.username());
    user.setFirstName(userDto.firstName());
    user.setLastName(userDto.lastName());

    Address address = new Address();
    address.setCity(userDto.address().city());
    address.setAreaCode(userDto.address().postalCode());
    address.setHouseNumber(userDto.address().houseNumber());
    address.setStreet(userDto.address().street());
    user.setAddress(address);

    user.setRoles(List.of(userRole));
    User returnUser = userRepository.save(user);
    sendVerificationToken(returnUser);
    return returnUser;
  }

  /**
   * Creates a staff account (admin operation). The email is pre-verified because an admin set the
   * account up, so the staff member only has to set up TOTP on first login. The praxis link is done
   * by the caller via StaffPraxisService.
   */
  public User createStaff(String username, String password, String firstName, String lastName) {
    if (this.userRepository.findByUsername(username).isPresent()) {
      throw new PublicExceptions.UsernameAlreadyExistsException();
    }
    isPasswordStrong(password);

    Role staffRole = this.roleService.getRole(PermissionRoles.STAFF);

    User user = new User();
    user.setPassword(passwordEncoder.encode(password));
    user.setUsername(username);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmailVerified(true);
    user.setRoles(List.of(staffRole));
    return userRepository.save(user);
  }

  /** Changes the login email (= username) after re-auth. The new address must be free. */
  public User changeEmail(String username, String newEmail, String currentPassword) {
    User user = verifyPassword(username, currentPassword);
    if (!username.equals(newEmail) && userRepository.findByUsername(newEmail).isPresent()) {
      throw new PublicExceptions.UsernameAlreadyExistsException();
    }
    user.setUsername(newEmail);
    return userRepository.save(user);
  }

  public User setAppointmentReminders(String username, boolean enabled) {
    User user = getUserByUsername(username);
    user.setAppointmentReminders(enabled);
    return userRepository.save(user);
  }

  /** Re-auth helper: checks the account password and returns the user. */
  public User verifyPassword(String username, String currentPassword) {
    User user = getUserByUsername(username);
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      // Public exception so a wrong password is a clean 400, not a 500.
      throw new PublicExceptions.IllegalArgumentsException("Current password is incorrect");
    }
    return user;
  }

  /**
   * Turns off the second factor after re-auth, clearing the active and any pending secret. The
   * account can then enroll a fresh authenticator via the normal totp setup/confirm flow.
   */
  public void disableTotp(String username, String currentPassword) {
    User user = verifyPassword(username, currentPassword);
    user.setTotpEnabled(false);
    user.setTotpSecret(null);
    user.setPendingTotpSecret(null);
    userRepository.save(user);
  }

  public void delete(User user) {
    userRepository.delete(user);
  }

  public void sendVerificationToken(User user) {
    String token = randomString(48);

    user.setVerificationToken(token);
    user.setVerificationSentAt(LocalDateTime.now());

    this.userRepository.saveAndFlush(user);

    // Sending the mail is best effort: a missing or unreachable SMTP server must
    // not fail registration. The token is already saved, so the user can still
    // verify later (resend via /verify-request) once mail is configured.
    try {
      this.emailService.sendVerificationEmail(user);
    } catch (Exception e) {
      log.warn("Could not send verification email: {}", e.getMessage());
    }
  }

  public void verifyVerificationToken(User user, String token) {
    Duration duration = Duration.between(user.getVerificationSentAt(), LocalDateTime.now());
    if (duration.minus(Duration.ofHours(3)).isNegative()) {
      if (token.equals(user.getVerificationToken())) {
        user.setEmailVerified(true);
        this.userRepository.save(user);
        return;
      }
      throw new PublicExceptions.InvalidVerificationTokenException();
    }
    throw new PublicExceptions.VerificationTokenExpiredException();
  }

  public void savePendingTotpSecret(String username, String secret) {
    User user = getUserByUsername(username);
    user.setPendingTotpSecret(secret);
    userRepository.save(user);
  }

  public void activateTotp(String username, String confirmedSecret) {
    User user = getUserByUsername(username);
    user.setTotpSecret(confirmedSecret);
    user.setPendingTotpSecret(null);
    user.setTotpEnabled(true);
    userRepository.save(user);
  }

  private boolean isPasswordStrong(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Password cannot be empty");
    }
    if (!PASSWORD_PATTERN.matcher(password).matches()) {
      throw new PublicExceptions.IllegalArgumentsException(
          "Password must be at least 8 characters long and contain at least one uppercase letter, "
              + "one lowercase letter, one digit, and one special character.");
    }
    return true;
  }

  public static String randomString(int length) {
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
    }
    return sb.toString();
  }

  @Autowired
  public UserService(
      UserRepository userRepository,
      RoleService roleService,
      PasswordEncoder passwordEncoder,
      EmailService emailService) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.roleService = roleService;
    this.emailService = emailService;
  }
}
