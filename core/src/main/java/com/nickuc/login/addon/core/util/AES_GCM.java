package com.nickuc.login.addon.core.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author mkyong
 * @link <a href="https://mkyong.com/java/java-aes-encryption-and-decryption/">Reference Link</a>
 * <p>
 * AES-GCM inputs - 12 bytes IV, need the same IV and secret keys for encryption and decryption.
 * <p>
 * The output consist of iv, password's salt, encrypted content and auth tag in the following
 * format: output = byte[] {i i i s s s c c c c c c ...}
 * <p>
 * i = IV bytes s = Salt bytes c = content bytes (encrypted content)
 */
public class AES_GCM {

  private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

  private static final int
      TAG_LENGTH_BIT = 128, // must be one of {128, 120, 112, 104, 96}
      IV_LENGTH_BYTE = 12,
      SALT_LENGTH_BYTE = 16;

  public static String encrypt(String content, String password) throws GeneralSecurityException {
    return encryptBytes(content.getBytes(StandardCharsets.UTF_8), password);
  }

  // return a base64 encoded AES encrypted text
  private static String encryptBytes(byte[] bytes, String password)
      throws GeneralSecurityException {

    // 16 bytes salt
    byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);

    // GCM recommended 12 bytes iv?
    byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

    // secret key from password
    SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

    Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

    // ASE-GCM needs GCMParameterSpec
    cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

    byte[] cipherText = cipher.doFinal(bytes);

    // prefix IV and Salt to cipher text
    byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
        .put(iv)
        .put(salt)
        .put(cipherText)
        .array();

    // string representation, base64, send this string to other for decryption.
    return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);

  }

  // we need the same password, salt and iv to decrypt it
  public static String decrypt(String content, String password) throws GeneralSecurityException {

    byte[] decode = Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8));

    // get back the iv and salt from the cipher text
    ByteBuffer bb = ByteBuffer.wrap(decode);

    byte[] iv = new byte[IV_LENGTH_BYTE];
    bb.get(iv);

    byte[] salt = new byte[SALT_LENGTH_BYTE];
    bb.get(salt);

    byte[] cipherText = new byte[bb.remaining()];
    bb.get(cipherText);

    // get back the aes key from the same password and salt
    SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

    Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

    cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

    byte[] plainText = cipher.doFinal(cipherText);

    return new String(plainText, StandardCharsets.UTF_8);
  }

  public static byte[] getRandomNonce(int numBytes) {
    byte[] nonce = new byte[numBytes];
    SecureGenerator.RANDOM.nextBytes(nonce);
    return nonce;
  }

  // Password derived AES 256 bits secret key
  public static SecretKey getAESKeyFromPassword(char[] password, byte[] salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    // iterationCount = 65536
    // keyLength = 256
    KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
  }
}