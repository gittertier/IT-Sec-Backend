package de.itsec.api.services;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TotpService {

  private final SecretGenerator secretGenerator;
  private final QrGenerator qrGenerator;
  private final CodeVerifier codeVerifier;

  public String generateSecret() {
    return secretGenerator.generate();
  }

  public String getQrCodeImageUri(String secret, String username) throws Exception {
    QrData data =
        new QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer("Impfterminportal RLP")
            // SHA1 for compatibility with manually entered secrets in auth apps
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
    return "data:image/png;base64,"
        + Base64.getEncoder().encodeToString(qrGenerator.generate(data));
  }

  public boolean verifyCode(String secret, String code) {
    return codeVerifier.isValidCode(secret, code);
  }
}
