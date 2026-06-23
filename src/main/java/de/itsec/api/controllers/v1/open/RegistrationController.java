package de.itsec.api.controllers.v1.open;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.UserPostRequestDto;
import de.itsec.api.data.dto.response.UserDto;
import de.itsec.api.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/public/register")
public class RegistrationController {

  private UserService userService;

  @PostMapping()
  public ResponseEntity<UserDto> register(@Valid @RequestBody UserPostRequestDto userDto) {

    User user = this.userService.createUser(userDto);

    return new ResponseEntity<>(UserDto.from(user), HttpStatus.CREATED);
  }

  @Autowired
  public RegistrationController(UserService userService) {
    this.userService = userService;
  }
}
