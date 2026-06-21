package de.itsec.api.controllers.v1;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.response.UserDto;
import de.itsec.api.services.UserService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the currently authenticated user so the client can verify its session and render the
 * protected area. Returns 401 (via the security entry point) when no session is active.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final UserService userService;

  @Autowired
  public MeController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public UserDto getCurrentUser(Principal principal) {
    User user = userService.getUserByUsername(principal.getName());
    return UserDto.from(user);
  }
}
