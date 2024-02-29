/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.packet.outgoing;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.packet.OutgoingPacket;
import com.nickuc.login.addon.core.platform.Settings;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class OutgoingHandshakePacket implements OutgoingPacket {

  private final byte[] challenge;
  private final Settings settings;

  @Override
  public void write(JsonObject out) {
    out.addProperty("challenge", Base64.getEncoder().encodeToString(challenge));
  }

  @Override
  public int id() {
    return 0;
  }
}
