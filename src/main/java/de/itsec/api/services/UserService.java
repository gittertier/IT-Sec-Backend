package de.itsec.api.services;

import de.itsec.api.PermissionRoles;
import de.itsec.api.data.authentication.Role;
import de.itsec.api.data.authentication.User;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.repositories.authentication.UserRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** UserService */
@Service
public class UserService {

  private static final String PASSWORD_REGEX =
      "^(?=.*[a-z])" // at least one lowercase letter
          + "(?=.*[A-Z])" // at least one uppercase letter
          + "(?=.*\\d)" // at least one digit
          + "(?=.*[^A-Za-z\\d])" // at least one special character (any non-alphanumeric)
          + ".{8,}$"; // at least 8 characters

  private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

  UserRepository userRepository;

  RoleService roleService;

  PasswordEncoder passwordEncoder;

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

  public User createUser(String username, String password) {

    if (this.userRepository.findByUsername(username).isPresent()) {
      throw new PublicExceptions.UsernameAlreadyExistsException();
    }

    isPasswordStrong(password);

    Role userRole = this.roleService.getRole(PermissionRoles.USER);

    User user = new User();
    user.setPassword(passwordEncoder.encode(password));
    user.setUsername(username);
    user.setEnabled(true);
    user.setRoles(List.of(userRole));
    return userRepository.save(user);
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

  @Autowired
  public UserService(
      UserRepository userRepository, RoleService roleService, PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.roleService = roleService;
  }
}
