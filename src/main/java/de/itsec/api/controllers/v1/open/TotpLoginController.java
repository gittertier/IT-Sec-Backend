package de.itsec.api.controllers.v1.open;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.TotpPendingAuthentication;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.services.TotpService;
import de.itsec.api.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/login")
public class TotpLoginController {

  private final TotpService totpService;
  private final UserService userService;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @PostMapping("/totp")
  public ResponseEntity<TotpVerifyResponse> verifyTotp(
      @RequestBody TotpVerifyRequest request, HttpServletRequest req, HttpServletResponse res) {

    HttpSession session = req.getSession(false);
    if (session == null) {
      throw new PublicExceptions.IllegalArgumentsException("No pending login session");
    }

    TotpPendingAuthentication pending =
        (TotpPendingAuthentication) session.getAttribute("TOTP_PENDING");
    if (pending == null) {
      throw new PublicExceptions.IllegalArgumentsException("No pending TOTP challenge");
    }

    String code = request.code();
    if (code == null || code.isBlank()) {
      throw new PublicExceptions.IllegalArgumentsException("Missing TOTP code");
    }

    User user = userService.getUserByUsername(pending.username());

    if (!totpService.verifyCode(user.getTotpSecret(), code)) {
      throw new PublicExceptions.IllegalArgumentsException("Invalid TOTP code");
    }

    session.removeAttribute("TOTP_PENDING");

    req.changeSessionId();

    UsernamePasswordAuthenticationToken fullAuth =
        new UsernamePasswordAuthenticationToken(pending.username(), null, pending.authorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(fullAuth);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, req, res);

    res.setStatus(HttpServletResponse.SC_OK);
    return ResponseEntity.ok(new TotpVerifyResponse("Login successful"));
  }

  public record TotpVerifyRequest(String code) {}

  public record TotpVerifyResponse(String message) {}
}
