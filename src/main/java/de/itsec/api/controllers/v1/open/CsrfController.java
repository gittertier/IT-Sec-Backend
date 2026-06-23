package de.itsec.api.controllers.v1.open;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/csrf")
public class CsrfController {

  @GetMapping
  public CsrfToken csrf(CsrfToken token) {
    return token;
  }
}
