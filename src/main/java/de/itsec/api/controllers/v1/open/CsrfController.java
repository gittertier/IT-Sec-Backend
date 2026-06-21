package de.itsec.api.controllers.v1.open;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint whose only purpose is to hand out the XSRF-TOKEN cookie before the first mutating
 * request. The cookie is emitted by {@code CsrfCookieFilter} on every response; this gives the
 * client a clean, side-effect-free GET to obtain it (e.g. right before registering or logging in).
 */
@RestController
@RequestMapping("/api/v1/public/csrf")
public class CsrfController {

  @GetMapping
  public ResponseEntity<Void> csrf() {
    return ResponseEntity.noContent().build();
  }
}
