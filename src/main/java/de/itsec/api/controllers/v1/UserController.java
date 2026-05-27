package de.itsec.api.controllers.v1;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.UserPostRequestDto;
import de.itsec.api.data.dto.response.UserDto;
import de.itsec.api.services.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Secured({"ROLE_ADMIN"})
public class UserController {

  private UserService userService;

  @GetMapping()
  public List<UserDto> getAllUsers() {
    return userService.getAllUsers().stream().map(UserDto::from).toList();
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
    User user = userService.getUserById(id);
    if (user == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(UserDto.from(user));
  }

  @PostMapping()
  public ResponseEntity<UserDto> createUser(
      @Valid @RequestBody UserPostRequestDto userPostRequest) {
    User user = this.userService.createUser(userPostRequest.username(), userPostRequest.password());
    return new ResponseEntity<>(UserDto.from(user), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }
}
