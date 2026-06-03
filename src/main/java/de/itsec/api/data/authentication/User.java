package de.itsec.api.data.authentication;

import de.itsec.api.crypto.StringCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(unique = true)
  private String username;

  @Convert(converter = StringCryptoConverter.class)
  private String email;

  private String password;
  private boolean enabled;
  private boolean tokenExpired;

  @ManyToMany
  @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
  private Collection<Role> roles;

  @Override
  public String toString() {
    return id + username;
  }
}
