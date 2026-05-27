package de.itsec.api.services;

import de.itsec.api.crypto.EncryptedMappingStore;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PseudoMappingService {

  private EncryptedMappingStore store;

  public PseudoMappingService(
      @Value("${pseudo.mapping.file}") Path file, @Value("${pseudo.key}") String key) {
    byte[] decodedKey = Base64.getDecoder().decode(key);
    try {
      this.store = new EncryptedMappingStore(file, new SecretKeySpec(decodedKey, "AES"));
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<String> pseudoIdFor(String userId) {
    return this.store.getPseudoId(userId);
  }

  public Optional<String> userIdFor(String pseudoId) {
    return this.store.getUserId(pseudoId);
  }
}
