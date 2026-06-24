package de.itsec.api.controllers.v1.open;

import de.itsec.api.data.authentication.User;
import de.itsec.api.exceptions.PublicExceptions;
import de.itsec.api.services.TotpService;
import de.itsec.api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

  @PostMapping("/totp/setup")
  public ResponseEntity<TotpSetupResponse> setupTotp(
      @AuthenticationPrincipal UserDetails userDetails) throws Exception {

    User user = userService.getUserByUsername(userDetails.getUsername());

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
      @RequestBody TotpConfirmRequest body, @AuthenticationPrincipal UserDetails userDetails) {

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
          "No pending TOTP setup — call /totp/setup first");
    }

    if (!totpService.verifyCode(pendingSecret, code)) {
      throw new PublicExceptions.IllegalArgumentsException(
          "Invalid code, check your authenticator and try again");
    }

    userService.activateTotp(user.getUsername(), pendingSecret);

    return ResponseEntity.ok(new TotpConfirmResponse("TOTP enabled successfully"));
  }

  public record TotpSetupResponse(String qrCode, String secret) {}

  public record TotpConfirmRequest(String code) {}

  public record TotpConfirmResponse(String message) {}
}
