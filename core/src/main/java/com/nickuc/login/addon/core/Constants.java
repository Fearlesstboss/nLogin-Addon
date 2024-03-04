/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
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
