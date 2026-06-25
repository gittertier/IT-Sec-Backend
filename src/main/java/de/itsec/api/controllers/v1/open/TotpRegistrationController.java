package de.itsec.api.controllers.v1.open;

import de.itsec.api.data.authentication.User;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.services.TotpService;
import de.itsec.api.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TotpRegistrationController {

  private final TotpService totpService;
  private final UserService userService;
  private final UserDetailsService userDetailsService;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @PostMapping("/totp/setup")
  public ResponseEntity<TotpSetupResponse> setupTotp(
      @AuthenticationPrincipal UserDetails userDetails) throws Exception {

    User user = userService.getUserByUsername(userDetails.getUsername());

    // Onboarding order: the email must be confirmed before 2FA can be set up.
    if (!user.isEmailVerified()) {
      throw new PublicExceptions.IllegalArgumentsException(
          "Please confirm your email before setting up 2FA");
    }
    if (user.isTotpEnabled()) {
      throw new PublicExceptions.IllegalArgumentsException(
          "TOTP is already enabled for this account");
    }

    String secret = totpService.generateSecret();
    userService.savePendingTotpSecret(user.getUsername(), secret);

    String qrUri = totpService.getQrCodeImageUri(secret, user.getUsername());

    return ResponseEntity.ok(new TotpSetupResponse(qrUri, secret));
  }

  @PostMapping("/totp/confirm")
  public ResponseEntity<TotpConfirmResponse> confirmTotp(
      @RequestBody TotpConfirmRequest body,
      @AuthenticationPrincipal UserDetails userDetails,
      HttpServletRequest req,
      HttpServletResponse res) {

    String code = body.code();
    if (code == null || code.isBlank()) {
      throw new PublicExceptions.IllegalArgumentsException("Missing code");
    }

    User user = userService.getUserByUsername(userDetails.getUsername());

    if (user.isTotpEnabled()) {
      throw new PublicExceptions.IllegalArgumentsException("TOTP already enabled");
    }

    String pendingSecret = user.getPendingTotpSecret();
    if (pendingSecret == null) {
      throw new PublicExceptions.IllegalArgumentsException(
          "No pending TOTP setup - call /totp/setup first");
    }

    if (!totpService.verifyCode(pendingSecret, code)) {
      throw new PublicExceptions.IllegalArgumentsException(
          "Invalid code, check your authenticator and try again");
    }

    userService.activateTotp(user.getUsername(), pendingSecret);

    // Onboarding is now complete (email verified + TOTP enabled), so lift the
    // session from ROLE_ONBOARDING to the account's real roles without a re-login.
    UserDetails full = userDetailsService.loadUserByUsername(user.getUsername());
    req.changeSessionId();
    UsernamePasswordAuthenticationToken fullAuth =
        new UsernamePasswordAuthenticationToken(full, null, full.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(fullAuth);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, req, res);

    return ResponseEntity.ok(new TotpConfirmResponse("TOTP enabled successfully"));
  }

  public record TotpSetupResponse(String qrCode, String secret) {}

  public record TotpConfirmRequest(String code) {}

  public record TotpConfirmResponse(String message) {}
}
