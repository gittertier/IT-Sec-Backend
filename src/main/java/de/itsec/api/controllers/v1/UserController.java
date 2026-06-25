package de.itsec.api.controllers.v1;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.response.UserDto;
import de.itsec.api.services.TerminService;
import de.itsec.api.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

  private final UserService userService;
  private final TerminService terminService;

  @PostMapping("/verify-email")
  public ResponseEntity<Void> verifyEmail(
      Principal principal, @RequestBody @Valid VerificationRequestDto requestBody) {
    User user = this.userService.getUserByUsername(principal.getName());
    this.userService.verifyVerificationToken(user, requestBody.token());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/verify-request")
  public ResponseEntity<Void> requestVerfication(Principal principal) {
    User user = this.userService.getUserByUsername(principal.getName());
    this.userService.sendVerificationToken(user);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserDto> getOwnProfile(Principal principal) {
    User user = this.userService.getUserByUsername(principal.getName());
    return ResponseEntity.ok(UserDto.from(user));
  }

  @PatchMapping("/me")
  public ResponseEntity<UserDto> patchOwnProfile(
      @RequestBody UserPatchDto patch, Principal principal) {
    User updated = this.userService.patchUser(principal.getName(), patch);
    return ResponseEntity.ok(UserDto.from(updated));
  }

  // --- Account settings (session bound, the account comes from the session) ---

  @PutMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @RequestBody @Valid PasswordChangeDto body, Principal principal) {
    userService.updatePassword(principal.getName(), body.currentPassword(), body.newPassword());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me/notifications")
  public ResponseEntity<NotificationPrefs> getNotifications(Principal principal) {
    User user = userService.getUserByUsername(principal.getName());
    return ResponseEntity.ok(new NotificationPrefs(user.isAppointmentReminders()));
  }

  @PutMapping("/me/notifications")
  public ResponseEntity<NotificationPrefs> updateNotifications(
      @RequestBody NotificationPrefs body, Principal principal) {
    User user =
        userService.setAppointmentReminders(principal.getName(), body.appointmentReminders());
    return ResponseEntity.ok(new NotificationPrefs(user.isAppointmentReminders()));
  }

  /**
   * Changes the login email after re-auth. The change ends the session so the user signs in again
   * with the new address (the username drives the session, so it must not go stale).
   */
  @PutMapping("/me/email")
  public ResponseEntity<MessageResponse> changeEmail(
      @RequestBody @Valid EmailChangeDto body, Principal principal, HttpServletRequest request) {
    userService.changeEmail(principal.getName(), body.email(), body.currentPassword());
    endSession(request);
    return ResponseEntity.ok(
        new MessageResponse("E-Mail geaendert. Bitte melden Sie sich neu an."));
  }

  /** Deletes the own account after re-auth: frees future bookings, removes the account, ends the session. */
  @PostMapping("/me/delete")
  public ResponseEntity<Void> deleteAccount(
      @RequestBody @Valid PasswordOnlyDto body, Principal principal, HttpServletRequest request) {
    User user = userService.verifyPassword(principal.getName(), body.currentPassword());
    terminService.releaseFutureBookings(user.getId());
    userService.delete(user);
    endSession(request);
    return ResponseEntity.noContent().build();
  }

  private void endSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  public record VerificationRequestDto(String token) {}

  public record UserPatchDto(
      String username, String firstName, String lastName, AddressPatchDto address) {}

  public record AddressPatchDto(
      String street, String city, String postalCode, String houseNumber) {}

  public record PasswordChangeDto(
      @NotEmpty String currentPassword, @NotEmpty String newPassword) {}

  public record EmailChangeDto(@NotEmpty String email, @NotEmpty String currentPassword) {}

  public record PasswordOnlyDto(@NotEmpty String currentPassword) {}

  public record NotificationPrefs(boolean appointmentReminders) {}

  public record MessageResponse(String message) {}

  @Autowired
  public UserController(UserService userService, TerminService terminService) {
    this.userService = userService;
    this.terminService = terminService;
  }
}
