package de.itsec.api.data.dto.response;

import de.itsec.api.data.authentication.Privilege;

public record PrivilegeDto(Long id, String name) {

  /**
   * Creates a PrivilegeDto from a Privilege entity.
   *
   * @param privilege The Privilege entity to convert
   * @return A new PrivilegeDto instance containing the privilege's information
   * @throws NullPointerException if the privilege parameter is null
   */
  public static PrivilegeDto from(Privilege privilege) {
    return new PrivilegeDto(privilege.getId(), privilege.getName());
  }
}
