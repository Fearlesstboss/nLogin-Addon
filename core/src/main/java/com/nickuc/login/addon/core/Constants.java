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

package com.nickuc.login.addon.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

  public static final String PREFIX = "[nLogin-Addon] ";

  public static final int MAIN_KEY_LENGTH = 192;
  public static final int DEFAULT_PASSWORD_LENGTH = 12;
  public static final int RSA_CHALLENGE_BITS = 32;

  public static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .create();

  public static final Gson GSON_PRETTY = new GsonBuilder()
      .disableHtmlEscaping()
      .setPrettyPrinting()
      .create();

  public static final String
      ADDON_MODERN_CHANNEL0 = "nlogin",
      ADDON_MODERN_CHANNEL1 = "addon";

}
