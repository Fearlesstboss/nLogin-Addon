/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.packet.incoming;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.packet.IncomingPacket;
import com.nickuc.login.addon.core.util.security.RSA;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class IncomingReadyPacket implements IncomingPacket {

  private UUID userId;
  private int maxAllowedData;

  private boolean userRegistered;
  private boolean requireSync;

  private PublicKey key;
  private byte[] signature;

  @Override
  public void read(JsonObject in) {
    userId = UUID.fromString(in.get("user-id").getAsString());
    maxAllowedData = in.get("max-allowed-data").getAsInt();

    JsonObject clientJson = in.getAsJsonObject("client");
    userRegistered = clientJson.get("user-registered").getAsBoolean();
    requireSync = userRegistered && clientJson.get("require-sync").getAsBoolean();

    JsonObject challengeJson = in.get("challenge").getAsJsonObject();
    key = RSA.getPublicKeyFromBase64(challengeJson.get("key").getAsString());
    signature = Base64.getDecoder().decode(challengeJson.get("signature").getAsString());
  }
}
