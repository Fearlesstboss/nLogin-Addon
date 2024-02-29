/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.packet.outgoing;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.packet.OutgoingPacket;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class OutgoingSyncDataPacket implements OutgoingPacket {

  private final String checksum;
  private final String key; // encrypted with Zero-Knowledge Encryption
  private final String data; // encrypted with Zero-Knowledge Encryption

  @Override
  public void write(JsonObject out) {
    out.addProperty("key", key);
    out.addProperty("data", data);
    out.addProperty("checksum", checksum);
  }

  @Override
  public int id() {
    return 1;
  }
}
