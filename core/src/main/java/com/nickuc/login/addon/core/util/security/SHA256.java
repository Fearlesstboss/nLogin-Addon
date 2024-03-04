/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.util.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {

  public static String hash(String str) {
    return hash(str.getBytes(StandardCharsets.UTF_8));
  }

  public static String hash(byte[] content) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.reset();
      messageDigest.update(content);
      byte[] digest = messageDigest.digest();
      return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean checksum(String plainText, String hashed) {
    return SHA256.hash(plainText).equals(hashed);
  }
}
