package de.itsec.api.data.dto.response;

import de.itsec.api.data.authentication.Role;

public record RoleDto(long id, String name) {
  public static RoleDto from(Role role) {
    return new RoleDto(role.getId(), role.getName());
  }
}
