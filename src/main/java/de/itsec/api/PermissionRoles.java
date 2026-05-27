package de.itsec.api;

public enum PermissionRoles {
  USER("ROLE_USER"),
  STAFF("ROLE_STAFF"),
  ADMIN("ROLE_ADMIN");

  private String name;

  private PermissionRoles(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

}
