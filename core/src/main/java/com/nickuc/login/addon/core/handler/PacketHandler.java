/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.handler;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.Constants;
import com.nickuc.login.addon.core.i18n.Message;
import com.nickuc.login.addon.core.model.Credentials;
import com.nickuc.login.addon.core.model.Server;
import com.nickuc.login.addon.core.model.Session;
import com.nickuc.login.addon.core.model.User;
import com.nickuc.login.addon.core.nLoginAddon;
import com.nickuc.login.addon.core.packet.incoming.IncomingReadyPacket;
import com.nickuc.login.addon.core.packet.incoming.IncomingServerStatusPacket;
import com.nickuc.login.addon.core.packet.incoming.IncomingServerStatusPacket.Status;
import com.nickuc.login.addon.core.packet.incoming.IncomingSyncDataPacket;
import com.nickuc.login.addon.core.packet.outgoing.OutgoingSyncDataPacket;
import com.nickuc.login.addon.core.packet.outgoing.OutgoingSyncRequestPacket;
import com.nickuc.login.addon.core.platform.Platform;
import com.nickuc.login.addon.core.util.security.AES_GCM;
import com.nickuc.login.addon.core.util.security.RSA;
import com.nickuc.login.addon.core.util.security.SHA256;
import com.nickuc.login.addon.core.util.security.SecureGenerator;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class PacketHandler {

  private final nLoginAddon addon;
  private final Platform platform;
  private final Credentials credentials;

  public void handleReady(final IncomingReadyPacket packet) {
    Session session = addon.getSessionManager().getCurrent();
    if (session == null) {
      return;
    }

    session.init(packet);

    User user = credentials.getUser(session.getUserId());
    Server server = user.getServer(session.getServerId());

    if (!verifyServerSignature(packet, session, server != null ? server.getKey() : null)) {
      Message.INVALID_SIGNATURE.notification(platform);
      return;
    }

    String message;
    boolean syncPasswords = addon.getSettings().isSyncPasswords();
    if (packet.isUserRegistered() && server == null) {
      if (syncPasswords) {
        platform.sendRequest(new OutgoingSyncRequestPacket());

        if (credentials.getLinkedToken() == null) {
          Message.RECOMMEND_LINK.display(platform);
          Message.RECOMMEND_LINK.notification(platform);
        }
      }
      return;
    }

    if (packet.isUserRegistered()) {
      message = "/login " + server.getPassword();
    } else {
      String password = SecureGenerator.generatePassword();
      server = user.updateServer(packet.getId(), packet.getKey(), password);
      message = "/register " + password + " " + password;
      session.setSyncRequired(true);
      Message.REGISTERING_PASSWORD.notification(platform);
    }

    session.setServer(server);

    if (syncPasswords && !session.isSyncRequired()) {
      session.setSyncRequired(packet.isRequireSync());
    }

    platform.sendMessage(message);
  }

  public void handleSync(final IncomingSyncDataPacket packet) {
    Session session = addon.getSessionManager().getCurrent();
    if (session == null) {
      return;
    }

    final String encryptedData = packet.getData();
    final String checksum = packet.getChecksum();

    String detectedKey = null;
    for (String key : credentials.getKeys()) {
      if (SHA256.checksum(key + encryptedData, checksum)) {
        detectedKey = key;
        break;
      }
    }

    if (detectedKey == null) {
      Message.BACKUP_INVALID_PASSWORD.display(platform);
      Message.BACKUP_INVALID_PASSWORD.notification(platform);
      addon.debug("Cannot find the appropriate main key for this server");
      return;
    }

    String data;
    try {
      data = AES_GCM.decryptFromBase64(packet.getData(), detectedKey);
    } catch (GeneralSecurityException e) {
      Message.BACKUP_CORRUPTED.display(platform);
      Message.BACKUP_CORRUPTED.notification(platform);
      addon.debug("Cannot decrypt the data provided by this server");
      return;
    }

    JsonObject json;
    try {
      json = Constants.GSON.fromJson(data, JsonObject.class);
    } catch (Exception e) {
      Message.BACKUP_CORRUPTED.display(platform);
      Message.BACKUP_CORRUPTED.notification(platform);
      addon.debug("Cannot decode the data provided by this server");
      return;
    }

    Server server = Server.deserialize(json);
    if (server != null) {
      session.setServer(server);
      session.setSyncRequired(true);
      credentials.getUser(session.getUserId()).updateServer(server);

      Message.DOWNLOADING_DATA.notification(platform);
      platform.sendMessage("/login " + server.getPassword());
    }
  }

  public void handleServerStatus(final IncomingServerStatusPacket packet) {
    Session session = addon.getSessionManager().getCurrent();
    if (session == null) {
      return;
    }

    Status status = packet.getStatus();
    switch (status) {
      case LOGIN_SUCCESSFUL: {
        session.authenticate();

        Server server = session.getServer();
        if (server == null) {
          UUID serverId = session.getServerId();
          PublicKey serverKey = session.getServerKey();
          String plainPassword = session.getPlainPassword();
          if (serverId != null && serverKey != null && plainPassword != null) {
            server = credentials.getUser(session.getUserId())
                .updateServer(serverId, serverKey, plainPassword);

            Message.SAVING_PASSWORD.notification(platform);
            uploadSyncData(session, server.serialize(new JsonObject()).toString());
          }
        } else if (session.isSyncRequired()) {
          uploadSyncData(session, server.serialize(new JsonObject()).toString());
        }
        break;
      }

      default: {
        addon.debug("Received server status: " + status);
        break;
      }
    }
  }

  private boolean verifyServerSignature(IncomingReadyPacket packet, Session session, @Nullable PublicKey storedPublicKey) {
    PublicKey remotePublicKey = session.getServerKey();
    if (remotePublicKey == null) {
      addon.debug("The server did not send its public key.");
      return false;
    }

    if (storedPublicKey != null) {
      if (!Arrays.equals(storedPublicKey.getEncoded(), remotePublicKey.getEncoded())) {
        addon.debug("The public key of the remote server is not the same as the stored one.");
        return false;
      }

      byte[] decrypt = RSA.decrypt(storedPublicKey, packet.getSignature());
      if (decrypt == null) {
        addon.debug(
            "RSA challenge verification failed: the signature provided by the server is invalid");
        return false;
      }

      if (!Arrays.equals(decrypt, session.getRsaChallenge())) {
        addon.debug(
            "RSA challenge verification failed: the signature provided by the server does not match");
        return false;
      }
    }
    return true;
  }

  private void uploadSyncData(Session session, String data) {
    if (!session.isAuthenticated()) {
      addon.debug("Attempted to send sync data while unauthenticated");
      return;
    }

    String mainKey = credentials.getMainKey();
    String encryptedData;
    try {
      encryptedData = AES_GCM.encryptToBase64(data, mainKey);
    } catch (GeneralSecurityException e) {
      Message.ENCRYPTION_FAILED.display(platform);
      Message.ENCRYPTION_FAILED.notification(platform);
      addon.error("Cannot encrypt sync data using the main key and main password", e);
      return;
    }

    String checksum = SHA256.hash(mainKey + encryptedData);
    platform.sendRequest(new OutgoingSyncDataPacket(encryptedData, checksum));
  }
}
