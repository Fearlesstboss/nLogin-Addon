package com.nickuc.login.addon.core.i18n;

import com.nickuc.login.addon.core.platform.Platform;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Message {

  INVALID_SIGNATURE("autologin.invalidSignature"),

  DOWNLOADING_DATA("backup.downloadingData"),
  ENCRYPTION_FAILED("backup.encryptionFailed"),
  BACKUP_INVALID_PASSWORD("backup.invalidPassword"),
  BACKUP_CORRUPTED("backup.corrupted"),

  REGISTERING_PASSWORD("password.registering"),
  SAVING_PASSWORD("password.saving"),
  MAIN_PASSWORD_REQUIRED("password.mainPasswordRequired");

  private final String key;

  public String toText(Platform platform, Object... params) {
    return platform.translate(key, params);
  }

  public void display(Platform platform, Object... params) {
    platform.displayMessage(toText(platform, params));
  }

  public void notification(Platform platform, Object... params) {
    platform.showNotification(toText(platform, params));
  }
}
