package de.itsec.api.crypto;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.SecretKey;

public class EncryptedMappingStore {
  private final Path file;
  private final SecretKey key;
  private final SecureRandom random = new SecureRandom();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  private Map<String, String> userToPseudo = new HashMap<>();
  private Map<String, String> pseudoToUser = new HashMap<>();

  public EncryptedMappingStore(Path file, SecretKey key)
      throws IOException, GeneralSecurityException {
    this.file = file;
    this.key = key;
    load();
  }

  public Optional<String> getPseudoId(String userId) {
    lock.readLock().lock();
    try {
      return Optional.ofNullable(userToPseudo.get(userId));
    } finally {
      lock.readLock().unlock();
    }
  }

  public Optional<String> getUserId(String pseudoId) {
    lock.readLock().lock();
    try {
      return Optional.ofNullable(pseudoToUser.get(pseudoId));
    } finally {
      lock.readLock().unlock();
    }
  }

  public void putMapping(String userId, String pseudoId)
      throws IOException, GeneralSecurityException {
    lock.writeLock().lock();
    try {
      Map<String, String> next = new HashMap<>(userToPseudo);

      String oldPseudo = next.put(userId, pseudoId);
      if (oldPseudo != null) {
        pseudoToUser.remove(oldPseudo);
      }

      String existingUser = pseudoToUser.get(pseudoId);
      if (existingUser != null && !existingUser.equals(userId)) {
        throw new IllegalArgumentException("Pseudo ID already assigned to another user");
      }

      persist(next);
      rebuildIndexes(next);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void removeMappingByUserId(String userId) throws IOException, GeneralSecurityException {
    lock.writeLock().lock();
    try {
      Map<String, String> next = new HashMap<>(userToPseudo);
      String pseudo = next.remove(userId);
      if (pseudo != null) {
        persist(next);
        rebuildIndexes(next);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void load() throws IOException, GeneralSecurityException {
    if (!Files.exists(file)) {
      rebuildIndexes(new HashMap<>());
      return;
    }
    byte[] bytes = Files.readAllBytes(file);
    String json = MappingCrypto.decrypt(bytes, key);
    Map<String, String> loaded = parseJson(json);
    rebuildIndexes(loaded);
  }

  private void persist(Map<String, String> data) throws IOException, GeneralSecurityException {
    String json = toJson(data);
    byte[] iv = new byte[12];
    random.nextBytes(iv);
    byte[] encrypted = MappingCrypto.encrypt(json, key, iv);

    Path dir = file.toAbsolutePath().getParent();
    Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
    try {
      Files.write(tmp, encrypted, StandardOpenOption.TRUNCATE_EXISTING);
      try (var ch = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
        ch.force(true);
      }
      Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  private void rebuildIndexes(Map<String, String> data) {
    this.userToPseudo = Map.copyOf(data);
    Map<String, String> reverse = new HashMap<>();
    for (var e : data.entrySet()) {
      reverse.put(e.getValue(), e.getKey());
    }
    this.pseudoToUser = Map.copyOf(reverse);
  }

  private static Map<String, String> parseJson(String json) {
    try {
      return new ObjectMapper().readValue(json, new TypeReference<Map<String, String>>() {});
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse mapping JSON", e);
    }
  }

  private static String toJson(Map<String, String> data) {
    try {
      return new ObjectMapper().writeValueAsString(data);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize mapping JSON", e);
    }
  }
}
