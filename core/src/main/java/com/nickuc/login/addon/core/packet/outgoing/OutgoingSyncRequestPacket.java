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
public class OutgoingSyncRequestPacket implements OutgoingPacket {

  @Override
  public void write(JsonObject out) {
  }

  @Override
  public int id() {
    return 2;
  }
}
