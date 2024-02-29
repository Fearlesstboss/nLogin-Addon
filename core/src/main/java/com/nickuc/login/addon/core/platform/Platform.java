/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.platform;

import com.nickuc.login.addon.core.handler.EventHandler;
import com.nickuc.login.addon.core.packet.OutgoingPacket;
import java.nio.file.Path;

public interface Platform {

  boolean isEnabled();

  Settings getSettings();

  Path getSettingsDirectory();

  String translate(String key, Object... params);

  void registerEvents(EventHandler handler);

  void sendRequest(OutgoingPacket outgoingPacket);

  void sendMessage(String message);

  void displayMessage(String message);

  void showNotification(String message);

  void info(String message);

  void error(String message, Throwable t);

}
