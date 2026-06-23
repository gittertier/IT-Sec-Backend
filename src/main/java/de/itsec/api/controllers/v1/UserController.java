package de.itsec.api.controllers.v1;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.response.UserDto;
import de.itsec.api.services.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

  private UserService userService;

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

  public record VerificationRequestDto(String token) {}

  public record UserPatchDto(
      String username, String firstName, String lastName, AddressPatchDto address) {}

  public record AddressPatchDto(
      String street, String city, String postalCode, String houseNumber) {}

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }
}
