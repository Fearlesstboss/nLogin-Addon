/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.handler.EventHandler;
import com.nickuc.login.addon.core.handler.PacketHandler;
import com.nickuc.login.addon.core.i18n.Message;
import com.nickuc.login.addon.core.manager.LinkManager;
import com.nickuc.login.addon.core.manager.LinkManager.SyncResponse;
import com.nickuc.login.addon.core.manager.SessionManager;
import com.nickuc.login.addon.core.model.Credentials;
import com.nickuc.login.addon.core.packet.PacketRegistry;
import com.nickuc.login.addon.core.platform.Platform;
import com.nickuc.login.addon.core.platform.Settings;
import com.nickuc.login.addon.core.util.io.IOUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.filechooser.FileSystemView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class nLoginAddon {

  private final Platform platform;
  private final @Getter SessionManager sessionManager = new SessionManager();
  private @Getter Credentials credentials;

  public void enable() {
    File credentailsFile = loadCredentials();

    PacketHandler packetHandler = new PacketHandler(this, platform, credentials);
    PacketRegistry packetRegistry = new PacketRegistry(packetHandler);
    LinkManager linkManager = new LinkManager(this, platform, credentials);
    platform.registerEvents(new EventHandler(this, platform, packetRegistry));

    getSettings().init(
        credentials.getEncryptionPassword(),
        linkManager::linkAccount,
        linkManager::unlinkAccount);

    final AtomicInteger shouldSync = new AtomicInteger(0);
    final AtomicBoolean firstRun = new AtomicBoolean(true);

    Timer timer = new Timer("nLogin Addon Scheduler");
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        String linkedToken = credentials.getLinkedToken();
        if (shouldSync.incrementAndGet() == 3 && linkedToken != null) {
          try {
            SyncResponse syncResponse = linkManager.sync(linkedToken, credentials.getLinkedEmail());
            platform.info("Remote Sync response: " + syncResponse);

            if (!firstRun.getAndSet(false) || !syncResponse.equals(SyncResponse.VALID_RESPONSE)) {
              platform.showNotification(syncResponse.equals(SyncResponse.VALID_RESPONSE) ?
                  Message.NOTIFICATION_SYNC_SUCCESS.toText(platform) :
                  Message.NOTIFICATION_SYNC_FAILED.toText(platform, syncResponse.getMessage(platform)));
            }
          } catch (Exception e) {
            error("Cannot sync backup data remotely", e);
          }
        }

        String encryptionPassword = getSettings().getEncryptionPassword();
        if (encryptionPassword != null && !encryptionPassword.trim().equals(credentials.getEncryptionPassword())) {
          credentials.setEncryptionPassword(encryptionPassword);
          shouldSync.set(1);
        }

        if (platform.isEnabled()) {
          try {
            credentials.save(credentailsFile);
          } catch (Exception e) {
            error("Cannot save credentials to " + credentailsFile.getAbsolutePath(), e);
          }
        }
      }
    }, 0, TimeUnit.SECONDS.toMillis(1));
  }

  public Settings getSettings() {
    return platform.getSettings();
  }

  public File loadCredentials() {
    File folder;

    // Try to use a folder outside ".minecraft" to keep the same credentials across different Minecraft launchers
    String osName = System.getProperty("os.name");
    if (osName != null && osName.startsWith("Windows")) {
      folder = new File(System.getenv("APPDATA") + File.separator + "nLogin");
    } else if ("Linux".equals(osName) || "LINUX".equals(osName)) {
      folder = new File(System.getProperty("user.home") + File.separator + ".nlogin");
    } else {
      folder = new File(FileSystemView.getFileSystemView().getDefaultDirectory() + File.separator + "nLogin");
    }

    // If the credentials cannot be accessed/created (no permission), use the settings directory
    if (!folder.exists() && !folder.mkdirs() || !folder.isDirectory()) {
      folder = platform.getSettingsDirectory().toFile();

      if (!folder.exists() && !folder.mkdirs()) {
        throw new SecurityException("Cannot access/create credentials directory: " + folder.getAbsolutePath());
      }
    }

    File file = new File(folder, "credentials.json");
    JsonObject json;
    try {
      if (file.exists() ? !file.isFile() : !file.createNewFile()) {
        throw new SecurityException("Cannot access/create credentials file: " + file.getAbsolutePath());
      }

      String content;
      try (FileInputStream fileInputStream = new FileInputStream(file)) {
        content = new String(IOUtil.readAll(fileInputStream), StandardCharsets.UTF_8);
      }

      json = !content.isEmpty() ? Constants.GSON.fromJson(content, JsonObject.class) : new JsonObject();
    } catch (IOException e) {
      throw new RuntimeException("Cannot read/create credentials file: " + file.getAbsolutePath(), e);
    }

    credentials = Credentials.deserialize(json);
    return file;
  }

  public void error(String message, Throwable t) {
    platform.error(message, t);
    if (getSettings().isDebug()) {
      platform.displayMessage("§4ERROR §enLogin-Addon: §f" + message);
    }
  }

  public void debug(String message) {
    if (getSettings().isDebug()) {
      platform.info(message);
      platform.displayMessage("§9DEBUG §enLogin-Addon: §f" + message);
    }
  }
}
