/*
 * The MIT License (MIT)
 *
 * Copyright © 2024 nLogin Addon Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
