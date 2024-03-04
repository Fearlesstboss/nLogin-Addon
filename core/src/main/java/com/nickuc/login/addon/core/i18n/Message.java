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
  RECOMMEND_LINK("password.recommendLink"),

  HTTP_INVALID_REQUEST("linking.http.invalidRequest"),
  HTTP_ALREADY_LINKED("linking.http.alreadyLinked"),
  HTTP_NO_LINKED_ACCOUNTS("linking.http.noLinkedAccounts"),
  HTTP_UNLINK_TOKEN_NOT_MATCH("linking.http.unlinkTokenNotMatch"),
  HTTP_LINK_SUCCESS("linking.http.linkSuccess"),
  HTTP_UNLINK_SUCCESS("linking.http.unlinkSuccess"),
  HTTP_LINK_FAILED("linking.http.linkFailed"),

  NOTIFICATION_SYNC_SUCCESS("linking.notification.syncSuccess"),
  NOTIFICATION_SYNC_FAILED("linking.notification.syncFailed"),

  RESPONSE_INVALID_TOKEN_OR_EMAIL("linking.response.invalidTokenOrEmail"),
  RESPONSE_ENCRYPTION_PASSWORD_REQUIRED("linking.response.encryptionPasswordRequired"),
  RESPONSE_ENCRYPTION_PASSWORD_INVALID("linking.response.encryptionPasswordInvalid"),
  RESPONSE_CORRUPTED_COMPRESSED_DATA("linking.response.corruptedCompressedData"),
  RESPONSE_MALFORMED_JSON("linking.response.malformedJson"),
  RESPONSE_VALID_RESPONSE("linking.response.validResponse"),
  RESPONSE_INVALID_CREDENTIALS("linking.response.invalidCredentials"),
  RESPONSE_PERMISSION_DENIED("linking.response.permissionDenied"),
  RESPONSE_TOO_MANY_REQUESTS("linking.response.tooManyRequests");

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
