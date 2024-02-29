/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.util;

import com.nickuc.login.addon.core.Constants;
import java.security.SecureRandom;
import java.util.Base64;

public class SecureGenerator {

  public static final SecureRandom RANDOM = new SecureRandom();

  private static final char[]
      LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray(),
      NUMBERS = "0123456789".toCharArray(),
      SYMBOLS = "^!@#$%&*".toCharArray(),
      ALL = merge(LETTERS, NUMBERS, SYMBOLS);

  public static String generateMainKey() {
    // 6 bits = 1 char
    int bytesLength = (Constants.MAIN_KEY_BYTES * 6) / 8;
    byte[] bytes = new byte[bytesLength];

    RANDOM.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static byte[] generateRSAChallenge() {
    byte[] random = new byte[Constants.RSA_CHALLENGE_BYTES];
    RANDOM.nextBytes(random);
    return random;
  }

  public static String generatePassword() {
    StringBuilder builder = new StringBuilder();

    final int length = Constants.DEFAULT_PASSWORD_LENGTH;
    for (int i = 0; i < length; i++) {
      builder.append(ALL[(int) (RANDOM.nextDouble() * ALL.length)]);
    }

    for (int i = 0; i < length; i++) {
      char c = builder.charAt(i);

      boolean numbers = false;
      boolean symbols = false;
      for (char number : NUMBERS) {
        if (c == number) {
          numbers = true;
          break;
        }
      }

      if (numbers) {
        for (char symbol : SYMBOLS) {
          if (c == symbol) {
            symbols = true;
            break;
          }
        }
      }

      if (!symbols) {
        int restrictedPos = (int) (RANDOM.nextDouble() * length);
        builder.setCharAt(restrictedPos, NUMBERS[(int) (RANDOM.nextDouble() * NUMBERS.length)]);
        while (true) {
          int position = (int) (RANDOM.nextDouble() * length);
          if (position == restrictedPos) {
            continue;
          }
          builder.setCharAt(position, SYMBOLS[(int) (RANDOM.nextDouble() * SYMBOLS.length)]);
          break;
        }
      }
    }

    return builder.toString();
  }

  private static char[] merge(char[]... chars) {
    int len = 0;
    for (char[] charArray : chars) {
      len += charArray.length;
    }

    char[] buf = new char[len];
    int index = 0;
    for (char[] charArray : chars) {
      for (char c : charArray) {
        buf[index++] = c;
      }
    }
    return buf;
  }
}
