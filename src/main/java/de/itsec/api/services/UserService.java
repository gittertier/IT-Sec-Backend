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
          + ".{8,}$"; // at least 8 characters

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
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }

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

  public User getUserById(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new RuntimeException("id not found"));
  }

  public User patchUser(String username, UserController.UserPatchDto patch) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

    if (patch.username() != null) {
      user.setUsername(patch.username());
    }
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
  public void deleteUser(Long id) {
    userRepository.deleteById(id);
  }

  /** Gets all users from the repository. */
  public List<User> getAllUsers() {
    return userRepository.findAll();
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
