package de.itsec.api;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotpConfig {

  @Bean
  public SecretGenerator secretGenerator() {
    return new DefaultSecretGenerator();
  }

  @Bean
  public QrGenerator qrGenerator() {
    return new ZxingPngQrGenerator();
  }

  @Bean
  public CodeVerifier codeVerifier() {
    // SHA1 must match the algorithm advertised in the QR (TotpService). Manually
    // typed keys assume SHA1, so using SHA256 here broke manual setup.
    DefaultCodeVerifier verifier =
        new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());
    verifier.setTimePeriod(30);
    // Allow only +/-1 step (was 2), so a captured code stays valid for a shorter
    // window - smaller replay surface.
    verifier.setAllowedTimePeriodDiscrepancy(1);
    return verifier;
  }
}
