package de.itsec.api.data.authentication;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "privileges")
@NoArgsConstructor
public class Privilege {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String name;

  @ManyToMany(mappedBy = "privileges")
  private Collection<Role> roles;

  public Privilege(String name) {
    this.name = name;
  }
}
