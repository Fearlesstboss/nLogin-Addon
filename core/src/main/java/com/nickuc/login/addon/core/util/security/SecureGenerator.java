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

  public static String generateKey() {
    // 6 bits = 1 char
    int bytesLength = (Constants.MAIN_KEY_LENGTH * 6) / 8;
    byte[] bytes = new byte[bytesLength];

    RANDOM.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static byte[] generateRSAChallenge() {
    return getRandomNonce(Constants.RSA_CHALLENGE_BITS / 8);
  }

  public static byte[] getRandomNonce(int numBytes) {
    byte[] nonce = new byte[numBytes];
    RANDOM.nextBytes(nonce);
    return nonce;
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
