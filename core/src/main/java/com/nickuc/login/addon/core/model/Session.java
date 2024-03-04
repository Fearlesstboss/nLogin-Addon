/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.model;

import com.nickuc.login.addon.core.packet.incoming.IncomingReadyPacket;
import java.security.PublicKey;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class Session {

  private final byte[] rsaChallenge;
  private UUID serverId;
  private UUID userId;
  private PublicKey serverKey;

  private @Setter String plainPassword;
  private @Setter boolean syncRequired;
  private @Setter Server server;
  private boolean authenticated;

  public void authenticate() {
    authenticated = true;
  }

  public void init(IncomingReadyPacket packet) {
    this.serverId = packet.getId();
    this.userId = packet.getUserId();
    this.serverKey = packet.getKey();
  }
}
