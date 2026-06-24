package de.itsec.api.data.dto.response;

import de.itsec.api.data.authentication.User;
import java.util.List;
import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String firstName,
    String lastName,
    boolean emailVerified,
    boolean totpEnabled,
    AddressDto address,
    List<RoleDto> roles) {

  /**
   * Creates a UserDto from a User entity. The frontend needs the name for the header, emailVerified
   * for the booking gate, and totpEnabled to know whether the account still has to set up 2FA.
   *
   * @param user The User entity to convert
   * @return A new UserDto instance containing the user's information
   */
  public static UserDto from(User user) {
    return new UserDto(
        user.getId(),
        user.getUsername(),
        user.getFirstName(),
        user.getLastName(),
        user.isEmailVerified(),
        user.isTotpEnabled(),
        AddressDto.from(user.getAddress()),
        user.getRoles().stream().map(RoleDto::from).toList());
  }
}
