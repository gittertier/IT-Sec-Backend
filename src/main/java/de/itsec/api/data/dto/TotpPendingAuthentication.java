package de.itsec.api.data.dto;

import java.io.Serializable;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record TotpPendingAuthentication(
    String username, Collection<? extends GrantedAuthority> authorities) implements Serializable {}
