package de.itsec.api.controllers.v1;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.UserPostRequestDto;
import de.itsec.api.data.dto.response.UserDto;
import de.itsec.api.data.termin.Praxis;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.repositories.termin.PraxisRepository;
import de.itsec.api.services.StaffPraxisService;
import de.itsec.api.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
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
public class AdminUserController {

  private final UserService userService;
  private final StaffPraxisService staffPraxisService;
  private final PraxisRepository praxisRepository;

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
    User user = this.userService.createUser(userPostRequest);
    return new ResponseEntity<>(UserDto.from(user), HttpStatus.CREATED);
  }

  /** Creates a staff account and binds it to a praxis. */
  @PostMapping("/staff")
  public ResponseEntity<UserDto> createStaff(@Valid @RequestBody StaffCreateRequest request) {
    Praxis praxis =
        praxisRepository
            .findById(request.praxisId())
            .orElseThrow(PublicExceptions.PraxisNotFoundException::new);
    User staff =
        userService.createStaff(
            request.username(), request.password(), request.firstName(), request.lastName());
    staffPraxisService.assign(staff.getId(), praxis);
    return new ResponseEntity<>(UserDto.from(staff), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  public record StaffCreateRequest(
      @NotNull @NotEmpty String username,
      @NotNull @NotEmpty String password,
      String firstName,
      String lastName,
      @NotNull UUID praxisId) {}

  @Autowired
  public AdminUserController(
      UserService userService,
      StaffPraxisService staffPraxisService,
      PraxisRepository praxisRepository) {
    this.userService = userService;
    this.staffPraxisService = staffPraxisService;
    this.praxisRepository = praxisRepository;
  }
}
