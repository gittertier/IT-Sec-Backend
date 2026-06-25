package de.itsec.api.services;

import de.itsec.api.data.authentication.Privilege;
import de.itsec.api.data.authentication.Role;
import de.itsec.api.data.authentication.User;
import de.itsec.api.repositories.authentication.UserRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userDetailsService")
@Transactional
public class ApiUserDetailsService implements UserDetailsService {

  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    // The real role (USER/STAFF/ADMIN) is ALWAYS granted, so the account's role
    // stays visible in the session, in /me and in audits - we never overwrite it.
    // While onboarding is unfinished (email not verified or TOTP not set up) we add
    // a ROLE_ONBOARDING marker on top. SecurityConfig denies every non-onboarding
    // endpoint as long as that marker is present, and that URL check runs before
    // method security, so a half-onboarded admin still cannot reach an
    // @Secured("ROLE_ADMIN") endpoint. ROLE_ONBOARDING is thus a status flag, not a
    // replacement for the role.
    List<GrantedAuthority> authorities = new ArrayList<>(getAuthorities(user.getRoles()));
    if (!(user.isEmailVerified() && user.isTotpEnabled())) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ONBOARDING"));
    }

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getPassword(), true, true, true, true, authorities);
  }

  private Collection<? extends GrantedAuthority> getAuthorities(Collection<Role> roles) {

    return getGrantedAuthorities(getPrivileges(roles));
  }

  private List<String> getPrivileges(Collection<Role> roles) {

    List<String> privileges = new ArrayList<>();
    List<Privilege> collection = new ArrayList<>();
    for (Role role : roles) {
      privileges.add(role.getName());
      collection.addAll(role.getPrivileges());
    }
    for (Privilege item : collection) {
      privileges.add(item.getName());
    }
    return privileges;
  }

  private List<GrantedAuthority> getGrantedAuthorities(List<String> privileges) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    for (String privilege : privileges) {
      authorities.add(new SimpleGrantedAuthority(privilege));
    }
    return authorities;
  }

  @Autowired
  public ApiUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }
}
