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
import com.nickuc.login.addon.core.util.AES_GCM;
import com.nickuc.login.addon.core.util.RSA;
import com.nickuc.login.addon.core.util.SHA256;
import com.nickuc.login.addon.core.util.SecureGenerator;
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

  public void handleReady(final IncomingReadyPacket packet) {
    Session session = addon.getSessionManager().getCurrent();
    if (session == null) {
      return;
    }

    session.init(packet);

    Credentials credentials = addon.getCredentials();
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
        if (!credentials.getMainPassword().isEmpty()) {
          platform.sendRequest(new OutgoingSyncRequestPacket());
        } else {
          Message.MAIN_PASSWORD_REQUIRED.display(platform);
          Message.MAIN_PASSWORD_REQUIRED.notification(platform);
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
      Message.REGISTERING_PASSWORD.notification(platform);
    }

    session.setServer(server);

    if (syncPasswords) {
      String mainPassword = credentials.getMainPassword();
      if (!mainPassword.isEmpty()) {
        boolean syncRequired = packet.isRequireSync() ||  !SHA256.checksum(mainPassword + user.getMainKey(), packet.getChecksum());
        session.setSyncRequired(syncRequired);
        addon.debug("Is Sync Required? " + syncRequired);
      }
    }

    platform.sendMessage(message);
  }

  public void handleSync(final IncomingSyncDataPacket packet) {
    Session session = addon.getSessionManager().getCurrent();
    if (session == null) {
      return;
    }

    final Credentials credentials = addon.getCredentials();
    final User user = credentials.getUser(session.getUserId());

    String mainPassword = credentials.getMainPassword();
    String mainKey = user.getMainKey();
    String checksum = session.getChecksum();

    if (!SHA256.checksum(mainKey + mainPassword, checksum)) {
      boolean detected = false;
      for (String testMainKey : user.getMainKeys()) {
        if (testMainKey.equals(mainKey)) {
          continue;
        }

        if (SHA256.checksum(testMainKey + mainPassword, checksum)) {
          detected = true;
          mainKey = testMainKey;
          break;
        }
      }

      if (!detected) {
        try {
          mainKey = AES_GCM.decrypt(packet.getKey(), mainPassword);
        } catch (GeneralSecurityException e) {
          Message.BACKUP_INVALID_PASSWORD.display(platform);
          Message.BACKUP_INVALID_PASSWORD.notification(platform);
          addon.debug("Cannot find the appropriate main key for this server");
          return;
        }
      }
    }

    String data;
    try {
      data = AES_GCM.decrypt(packet.getData(), mainKey);
    } catch (GeneralSecurityException e) {
      Message.BACKUP_CORRUPTED.display(platform);
      Message.BACKUP_CORRUPTED.notification(platform);
      addon.debug("Cannot decrypt the data provided by this server");
      return;
    }

    JsonObject json = Constants.GSON.fromJson(data, JsonObject.class);
    Server server = Server.deserialize(json);
    if (server != null) {
      session.setServer(server);
      session.setSyncRequired(true);
      user.updateServer(server);
      user.addMainKey(mainKey);

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
            server = addon.getCredentials().getUser(session.getUserId())
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

    Credentials credentials = addon.getCredentials();
    String mainPassword = credentials.getMainPassword();
    if (mainPassword.isEmpty()) {
      return;
    }

    String mainKey = credentials.getUser(session.getUserId()).getMainKey();
    String checksum = SHA256.hash(SHA256.hash(mainPassword + mainKey));
    String encryptedMainKey, encryptedData;
    try {
      encryptedMainKey = AES_GCM.encrypt(mainKey, mainPassword);
      encryptedData = AES_GCM.encrypt(data, mainKey);
    } catch (GeneralSecurityException e) {
      Message.ENCRYPTION_FAILED.display(platform);
      Message.ENCRYPTION_FAILED.notification(platform);
      addon.error("Cannot encrypt sync data using the main key and main password", e);
      return;
    }

    platform.sendRequest(new OutgoingSyncDataPacket(checksum, encryptedMainKey, encryptedData));
  }
}
