package de.itsec.api.services;

import de.itsec.api.crypto.EncryptedMappingStore;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
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

  public Optional<UUID> pseudoIdFor(UUID userId) {
    return this.store.getPseudoId(userId);
  }

  public Optional<UUID> userIdFor(UUID pseudoId) {
    return this.store.getUserId(pseudoId);
  }

  /**
   * Returns the pseudonym for a user, creating and persisting a fresh one on first use.
   *
   * <p>The mapping between the real user id and the pseudonym only ever lives in the encrypted
   * store, so callers can safely persist the returned pseudonym without leaking user identity.
   *
   * @param userId the real user id
   * @return the stable pseudonym for that user
   */
  public synchronized UUID getOrCreatePseudoIdFor(UUID userId) {
    Optional<UUID> existing = this.store.getPseudoId(userId);
    if (existing.isPresent()) {
      return existing.get();
    }
    UUID pseudoId = UUID.randomUUID();
    try {
      this.store.putMapping(userId, pseudoId);
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    return pseudoId;
  }
}
