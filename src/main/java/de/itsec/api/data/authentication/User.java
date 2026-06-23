package de.itsec.api.data.authentication;

import de.itsec.api.crypto.StringCryptoConverter;
import de.itsec.api.data.Address;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
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
  // @Convert(converter = StringCryptoConverter.class)
  private String username;

  private String password;

  @Convert(converter = StringCryptoConverter.class)
  private String firstName;

  @Convert(converter = StringCryptoConverter.class)
  private String lastName;

  private boolean emailVerified;

  private String verificationToken;

  private LocalDateTime verificationSentAt;

  @ManyToMany
  @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
  private Collection<Role> roles;

  @ManyToOne(cascade = CascadeType.ALL)
  private Address address;


  public String getFullName() {
    return this.getFirstName() + " " + this.getLastName();
  }

  @Override
  public String toString() {
    return id + username;
  }
}
