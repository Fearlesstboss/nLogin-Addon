/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {

  public static String hash(String content) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.reset();
      messageDigest.update(content.getBytes());
      byte[] digest = messageDigest.digest();
      return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean checksum(String plainText, String hashed) {
    return SHA256.hash(SHA256.hash(plainText)).equals(hashed);
  }
}
