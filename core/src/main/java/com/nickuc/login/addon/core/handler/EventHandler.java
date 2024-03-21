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

package com.nickuc.login.addon.core.handler;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.Constants;
import com.nickuc.login.addon.core.model.Session;
import com.nickuc.login.addon.core.nLoginAddon;
import com.nickuc.login.addon.core.packet.IncomingPacket;
import com.nickuc.login.addon.core.packet.PacketRegistry;
import com.nickuc.login.addon.core.packet.outgoing.OutgoingHandshakePacket;
import com.nickuc.login.addon.core.platform.Platform;
import com.nickuc.login.addon.core.platform.Settings;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventHandler {

  private static final Set<String> AUTH_COMMANDS = new HashSet<>(Arrays.asList(
      "/login",
      "/logar",
      "/log",
      "/register",
      "/registrar",
      "/reg"
  ));

  private final nLoginAddon addon;
  private final Platform platform;
  private final PacketRegistry packetRegistry;

  public void handleJoin() {
    try {
      if (!addon.getSettings().isEnabled()) {
        return;
      }

      Session session = addon.getSessionManager().newSession();
      platform.sendRequest(
          new OutgoingHandshakePacket(session.getRsaChallenge(), addon.getSettings()));
    } catch (Throwable t) {
      addon.error("Error while handling join: " + t.getMessage(), t);
    }
  }

  public void handleQuit() {
    try {
      addon.getSessionManager().invalidate();
    } catch (Throwable t) {
      addon.error("Error while handling quit: " + t.getMessage(), t);
    }
  }

  public boolean handleChat(String message) {
    try {
      Settings settings = addon.getSettings();
      if (!settings.isEnabled() || !settings.isSaveLogin() || message.isEmpty()) {
        return false;
      }

      Session session = addon.getSessionManager().getCurrent();
      if (session == null) {
        return false;
      }

      if (message.charAt(0) == '/') {
        String[] parts = message.split(" ");
        if (parts.length > 1) {
          String command = parts[0].toLowerCase();
          if (AUTH_COMMANDS.contains(command)) {
            session.setPlainPassword(parts[1]);
          }
        }
      }
      return false;
    } catch (Throwable t) {
      addon.error("Error while handling chat: " + t.getMessage(), t);
      return false;
    }
  }

  public void handleCustomMessage(byte[] payload) {
    if (!addon.getSettings().isEnabled()) {
      return;
    }

    try {
      String data = new String(payload, StandardCharsets.UTF_8);
      JsonObject in = Constants.GSON.fromJson(data, JsonObject.class);
      if (!in.has("id") || !in.has("data")) {
        return;
      }

      int id = in.get("id").getAsInt();
      JsonObject inData = in.get("data").getAsJsonObject();

      IncomingPacket incomingPacket = packetRegistry.create(inData, id);
      if (incomingPacket == null) {
        addon.debug("Skipping unknown packet with ID " + id);
        return;
      }

      try {
        incomingPacket.read(inData);
      } catch (Exception e) {
        addon.error("Cannot decode packet " + incomingPacket.getClass().getSimpleName() + ": " + e.getMessage(), e);
        return;
      }

      try {
        packetRegistry.handle(incomingPacket, id);
        addon.debug("Packet " + incomingPacket.getClass().getSimpleName() + " successfully handled");
      } catch (Exception e) {
        addon.error("Cannot handle packet " + incomingPacket.getClass().getSimpleName() + ": " + e.getMessage(), e);
      }
    } catch (Throwable t) {
      addon.error("Error while handling custom message: " + t.getMessage(), t);
    }
  }
}
