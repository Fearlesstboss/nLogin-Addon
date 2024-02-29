/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.handler;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.Constants;
import com.nickuc.login.addon.core.model.Session;
import com.nickuc.login.addon.core.nLoginAddon;
import com.nickuc.login.addon.core.packet.IncomingPacket;
import com.nickuc.login.addon.core.packet.PacketRegistry;
import com.nickuc.login.addon.core.packet.outgoing.OutgoingHandshakePacket;
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

  public void handleJoin() {
    try {
      if (!addon.getSettings().isEnabled()) {
        return;
      }

      Session session = addon.getSessionManager().newSession();
      addon.getPlatform().sendRequest(
          new OutgoingHandshakePacket(session.getRsaChallenge(), addon.getSettings()));
    } catch (Exception e) {
      addon.error("Error while handling join: " + e.getMessage(), e);
    }
  }

  public void handleQuit() {
    try {
      addon.getSessionManager().invalidate();
    } catch (Exception e) {
      addon.error("Error while handling quit: " + e.getMessage(), e);
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
    } catch (Exception e) {
      addon.error("Error while handling chat: " + e.getMessage(), e);
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

      PacketRegistry packetRegistry = addon.getPacketRegistry();

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
    } catch (Exception e) {
      addon.error("Error while handling custom message: " + e.getMessage(), e);
    }
  }
}
