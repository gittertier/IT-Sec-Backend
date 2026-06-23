package de.itsec.api.data.dto.response;

import de.itsec.api.data.authentication.User;
import java.util.List;
import java.util.UUID;

public record UserDto(UUID id, String username, AddressDto address, List<RoleDto> roles) {

  /**
   * Creates a UserDto from a User entity.
   *
   * @param user The User entity to convert
   * @return A new UserDto instance containing the user's information
   * @throws IllegalArgumentException if the user is null or if any required field is null or blank
   */
  public static UserDto from(User user) {
    return new UserDto(
        user.getId(),
        user.getUsername(),
        AddressDto.from(user.getAddress()),
        user.getRoles().stream().map(RoleDto::from).toList());
  }
}
