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
public class IncomingServerStatusPacket implements IncomingPacket {

  private Status status;

  @Override
  public void read(JsonObject in) {
    status = Status.byId(in.get("code").getAsInt());
  }

  public enum Status {
    LOGIN_SUCCESSFUL,
    RSA_CHALLENGE_REJECTED,
    SYNC_REQUEST_REJECTED,
    CHECKSUM_REJECTED,
    UNKNOWN;

    public static Status byId(int id) {
      return id >= 0 && id < values().length ? values()[id] : UNKNOWN;
    }
  }
}