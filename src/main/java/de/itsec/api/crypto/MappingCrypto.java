package de.itsec.api.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class MappingCrypto {

  private MappingCrypto() {}

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_BYTES = 12;

  public static String decrypt(byte[] fileBytes, SecretKey key) throws GeneralSecurityException {
    if (fileBytes.length < IV_BYTES + 1) {
      throw new GeneralSecurityException("Encrypted file too short");
    }

    byte[] iv = Arrays.copyOfRange(fileBytes, 0, IV_BYTES);
    byte[] ciphertextAndTag = Arrays.copyOfRange(fileBytes, IV_BYTES, fileBytes.length);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));

    byte[] plaintext = cipher.doFinal(ciphertextAndTag);
    return new String(plaintext, StandardCharsets.UTF_8);
  }

  public static byte[] encrypt(String plaintext, SecretKey key, byte[] iv)
      throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
    byte[] ciphertextAndTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

    ByteBuffer out = ByteBuffer.allocate(IV_BYTES + ciphertextAndTag.length);
    out.put(iv);
    out.put(ciphertextAndTag);
    return out.array();
  }
}
