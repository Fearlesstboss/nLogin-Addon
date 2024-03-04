/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.packet.incoming;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.packet.IncomingPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class IncomingSyncDataPacket implements IncomingPacket {

  private String data; // encrypted with Zero-Knowledge Encryption
  private String checksum;

  @Override
  public void read(JsonObject in) {
    data = in.get("data").getAsString();
    checksum = in.get("checksum").getAsString();
  }
}
