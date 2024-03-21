/*
 * The MIT License (MIT)
 *
 * Copyright © 2024 nLogin Addon Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nickuc.login.addon.core.util.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import org.jetbrains.annotations.Nullable;

public class RSA {

  public static PublicKey getPublicKeyFromBase64(String base64) {
    return getPublicKeyFromBytes(Base64.getDecoder().decode(base64));
  }

  @Nullable
  public static PublicKey getPublicKeyFromBytes(byte[] bytes) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
      return keyFactory.generatePublic(publicKeySpec);
    } catch (GeneralSecurityException e) {
      return null;
    }
  }

  @Nullable
  public static byte[] decrypt(PublicKey publicKey, byte[] data) {
    try {
      Cipher decryptCipher = Cipher.getInstance("RSA");
      decryptCipher.init(2, publicKey);
      return decryptCipher.doFinal(data);
    } catch (GeneralSecurityException e) {
      return null;
    }
  }
}
