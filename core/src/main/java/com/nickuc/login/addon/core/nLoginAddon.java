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
import com.nickuc.login.addon.core.manager.SessionManager;
import com.nickuc.login.addon.core.model.Credentials;
import com.nickuc.login.addon.core.packet.PacketRegistry;
import com.nickuc.login.addon.core.platform.Platform;
import com.nickuc.login.addon.core.platform.Settings;
import com.nickuc.login.addon.core.util.IOUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.swing.filechooser.FileSystemView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class nLoginAddon {

  private final Platform platform;
  private final SessionManager sessionManager = new SessionManager();
  private PacketHandler packetHandler;
  private PacketRegistry packetRegistry;
  private Credentials credentials;

  public void enable() {
    loadCredentials();
    packetHandler = new PacketHandler(this, platform);
    packetRegistry = new PacketRegistry(packetHandler);
    platform.registerEvents(new EventHandler(this));
    getSettings().setMainPassword(credentials.getMainPassword());
  }

  public Settings getSettings() {
    return platform.getSettings();
  }

  public void loadCredentials() {
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

    Timer timer = new Timer("nLoginAddon$Save");
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        String mainPassword = getSettings().getMainPassword();
        if (mainPassword != null && !mainPassword.isEmpty() && !mainPassword.trim().equals(credentials.getMainPassword())) {
          credentials.setMainPassword(mainPassword.trim());
        }

        if (platform.isEnabled()) {
          try {
            credentials.save(file);
          } catch (Exception e) {
            error("Cannot save credentials to " + file.getAbsolutePath(), e);
          }
        }
      }
    }, 0, TimeUnit.SECONDS.toMillis(3));
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
