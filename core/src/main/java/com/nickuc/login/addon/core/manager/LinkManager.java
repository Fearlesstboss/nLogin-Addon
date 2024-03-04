package com.nickuc.login.addon.core.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.Constants;
import com.nickuc.login.addon.core.i18n.Message;
import com.nickuc.login.addon.core.model.Credentials;
import com.nickuc.login.addon.core.nLoginAddon;
import com.nickuc.login.addon.core.platform.Platform;
import com.nickuc.login.addon.core.util.io.IOUtil;
import com.nickuc.login.addon.core.util.io.ZipUtils;
import com.nickuc.login.addon.core.util.io.http.HttpClient;
import com.nickuc.login.addon.core.util.io.http.HttpResponse;
import com.nickuc.login.addon.core.util.security.AES_GCM;
import com.nickuc.login.addon.core.util.security.SHA256;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.labymod.api.Laby;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class LinkManager {

  private static final String HTTP_200_RESPONSE = "HTTP/1.1 200 OK\nContent-Length: %d\nContent-Type: text/plain; charset=utf-8\n\n%s";

  private static final String DOWNLOAD_ENDPOINT = "https://api.nickuc.com/v6/nlogin/addon/download%s";
  private static final String UPLOAD_ENDPOINT = "https://api.nickuc.com/v6/nlogin/addon/upload?checksum=%s";

  private static final String LINK_URL = "https://www.nickuc.com/panel/addon_link";
  private static final String UNLINK_URL = "https://www.nickuc.com/panel/addon_link?action=unlink&token=%s";

  private final AtomicBoolean listening = new AtomicBoolean();
  private final AtomicBoolean uiLock = new AtomicBoolean();

  private final nLoginAddon addon;
  private final Platform platform;
  private final Credentials credentials;

  // All data uploaded to cloud backup servers (only if an account is linked)
  private JsonObject uploadData(Set<String> keysSet) {
    JsonObject out = new JsonObject();

    JsonArray keys = new JsonArray();
    for (String key : keysSet) {
      keys.add(key);
    }
    out.add("keys", keys);

    return out;
  }

  @Nullable
  private HttpResponse upload(String token) {
    Set<String> keys = credentials.getKeys();
    String keysChecksum = keysChecksum(keys);
    if (keysChecksum == null) {
      return null;
    }

    HttpClient client = HttpClient
        .create()
        .connectTimeout(5000)
        .readTimeout(5000)
        .header("Content-Type", "application/octet-stream")
        .token(token);

    byte[] data = ZipUtils.compress(uploadData(keys).toString().getBytes(StandardCharsets.UTF_8));

    // Data will be encrypted with Zero-Knowledge Encryption if the user has an encryption password defined
    String encryptionPassword = credentials.getEncryptionPassword();
    boolean shouldEncrypt = encryptionPassword != null && !encryptionPassword.isEmpty();
    if (shouldEncrypt) {
      try {
        data = AES_GCM.encrypt(data, encryptionPassword);
      } catch (GeneralSecurityException e) {
        throw new RuntimeException("Cannot encrypt sync request data", e);
      }
    }

    // Prepend "encrypted" to facilitate downloading later
    byte[] out = new byte[1 + data.length];
    out[0] = (byte) (shouldEncrypt ? 1 : 0);
    System.arraycopy(data, 0, out, 1, data.length);

    return client.post(String.format(UPLOAD_ENDPOINT, keysChecksum), out);
  }

  private HttpResponse download(String token) {
    HttpClient client = HttpClient
        .create()
        .connectTimeout(5000)
        .readTimeout(5000)
        .token(token);

    Set<String> keys = credentials.getKeys();
    String keysChecksum = keysChecksum(keys);

    String params = !keysChecksum.isEmpty() ? "?checksum=" + keysChecksum : "";
    return client.get(String.format(DOWNLOAD_ENDPOINT, params));
  }

  public SyncResponse sync(String token, String email) {
    if (token == null || email == null) {
      return SyncResponse.INVALID_TOKEN_OR_EMAIL;
    }

    HttpResponse download = download(token);
    switch (download.responseCode()) {
      case 200:
      case 201: {
        credentials.setLinkedToken(token);
        credentials.setLinkedEmail(email);

        boolean shouldUpload = download.responseCode() == 201;
        byte[] content = download.content();
        if (content.length > 1) {
          byte[] data = new byte[content.length - 1];
          System.arraycopy(content, 1, data, 0, data.length);

          boolean encrypted = content[0] == 1;
          if (encrypted) {
            String encryptionPassword = credentials.getEncryptionPassword();
            if (encryptionPassword == null || encryptionPassword.isEmpty()) {
              return SyncResponse.ENCRYPTION_PASSWORD_REQUIRED;
            }

            try {
              data = AES_GCM.decrypt(data, encryptionPassword);
            } catch (GeneralSecurityException e) {
              return SyncResponse.ENCRYPTION_PASSWORD_INVALID;
            }
          }

          try {
            data = ZipUtils.decompress(data);
          } catch (Exception e) {
            return SyncResponse.CORRUPTED_COMPRESSED_DATA;
          }

          try {
            JsonObject in = Constants.GSON.fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);

            Set<String> keys = new LinkedHashSet<>();
            if (in.has("keys")) {
              JsonArray keysJson = in.getAsJsonArray("keys");
              for (int i = 0; i < keysJson.size(); i++) {
                keys.add(keysJson.get(i).getAsJsonPrimitive().getAsString());
              }
            }

            // Restore saved keys
            keys.forEach(credentials::addKey);

            shouldUpload = true;
          } catch (Exception e) {
            return SyncResponse.MALFORMED_JSON;
          }
        }

        if (shouldUpload) {
          HttpResponse upload = upload(token);
          if (upload != null && upload.responseCode() != 200) {
            String contentUTF8 = upload.contentUTF8();
            return new SyncResponse(null, "upload " + upload.responseCode() + " " + (contentUTF8 != null ? contentUTF8 : "null"));
          }
        }

        return SyncResponse.VALID_RESPONSE;
      }

      case 401: {
        credentials.setLinkedToken(null);
        credentials.setLinkedEmail(null);
        return SyncResponse.INVALID_CREDENTIALS;
      }

      case 403: {
        return SyncResponse.PERMISSION_DENIED;
      }

      case 429: {
        return SyncResponse.TOO_MANY_REQUESTS;
      }

      default: {
        String contentUTF8 = download.contentUTF8();
        return new SyncResponse(null, "download " + download.responseCode() + " " + (contentUTF8 != null ? contentUTF8 : "null"));
      }
    }
  }

  private String keysChecksum(Set<String> keys) {
    List<String> sortedKeys = new ArrayList<>(keys);
    Collections.sort(sortedKeys);

    String checksum = "";
    for (String key : sortedKeys) {
      checksum = SHA256.hash(checksum + key);
    }
    return checksum;
  }

  public String handleHttpRequest(Map<String, String> params) {
    String action = params.get("action");
    String token = params.get("token");
    String email = params.get("email");
    if (action == null || token == null || email == null) {
      return Message.HTTP_INVALID_REQUEST.toText(platform);
    }

    switch (action) {
      case "link":
        if (token.equals(credentials.getLinkedToken())) {
          return Message.HTTP_ALREADY_LINKED.toText(platform, credentials.getLinkedEmail());
        }

        SyncResponse syncResponse = sync(token, email);
        return syncResponse.equals(SyncResponse.VALID_RESPONSE) ?
            Message.HTTP_LINK_SUCCESS.toText(platform) :
            Message.HTTP_LINK_FAILED.toText(platform, syncResponse.message);

      case "unlink":
        if (credentials.getLinkedToken() == null) {
          return Message.HTTP_NO_LINKED_ACCOUNTS.toText(platform);
        }

        if (!token.equals(credentials.getLinkedToken())) {
          return Message.HTTP_UNLINK_TOKEN_NOT_MATCH.toText(platform);
        }

        credentials.setLinkedToken(null);
        credentials.setLinkedEmail(null);
        return Message.HTTP_UNLINK_SUCCESS.toText(platform);

      default:
        return "Unsupported action!";
    }
  }

  public void listenLocalHttpRequests() {
    if (listening.getAndSet(true)) {
      return;
    }

    new Thread(() -> {
      try (ServerSocket server = new ServerSocket(8047); Socket client = server.accept()) {
        InputStream inputStream = client.getInputStream();
        byte[] bytes = IOUtil.readAll(inputStream);
        if (bytes.length == 0) {
          return;
        }

        String[] request = new String(bytes, StandardCharsets.UTF_8).split(" ");
        if (request.length < 2) {
          return;
        }

        String path = request[1];
        if (path.length() < 3) {
          return;
        }

        String[] queryString = path.substring(2).split("&");

        Map<String, String> params = new HashMap<>();
        for (String query : queryString) {
          if (query.isEmpty()) {
            continue;
          }

          String[] parts = query.split("=");
          String value = null;
          if (parts.length == 2) {
            value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
          }
          params.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), value);
        }

        String httpReplyContent = handleHttpRequest(params);

        OutputStream outputStream = client.getOutputStream();
        outputStream.write(String.format(HTTP_200_RESPONSE, httpReplyContent.length(), httpReplyContent).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      } catch (Throwable e) {
        addon.error("Cannot listen to local HTTP requests: " + e.getMessage(), e);
      } finally {
        listening.set(false);
      }
    }, "nLogin Addon Local HTTP Server").start();
  }

  public void linkAccount() {
    if (uiLock.getAndSet(true)) {
      return;
    }

    try {
      String linkedToken = credentials.getLinkedToken();
      if (linkedToken != null) {
        SyncResponse syncResponse = sync(linkedToken, credentials.getLinkedEmail());
        platform.showNotification(syncResponse.equals(SyncResponse.VALID_RESPONSE) ?
            Message.NOTIFICATION_SYNC_SUCCESS.toText(platform) :
            Message.NOTIFICATION_SYNC_FAILED.toText(platform, syncResponse.message));
        return;
      }

      listenLocalHttpRequests();
      Laby.labyAPI().minecraft().chatExecutor().openUrl(LINK_URL);
    } finally {
      uiLock.set(false);
    }
  }

  public void unlinkAccount() {
    if (uiLock.getAndSet(true)) {
      return;
    }

    try {
      String linkedToken = credentials.getLinkedToken();
      if (linkedToken == null) {
        return;
      }

      listenLocalHttpRequests();
      Laby.labyAPI().minecraft().chatExecutor().openUrl(String.format(UNLINK_URL, linkedToken));
    } finally {
      uiLock.set(false);
    }
  }

  @ToString
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @Getter
  public static class SyncResponse {
    public static final SyncResponse INVALID_TOKEN_OR_EMAIL = new SyncResponse(Message.RESPONSE_INVALID_TOKEN_OR_EMAIL);
    public static final SyncResponse ENCRYPTION_PASSWORD_REQUIRED = new SyncResponse(Message.RESPONSE_ENCRYPTION_PASSWORD_REQUIRED);
    public static final SyncResponse ENCRYPTION_PASSWORD_INVALID = new SyncResponse(Message.RESPONSE_ENCRYPTION_PASSWORD_INVALID);
    public static final SyncResponse CORRUPTED_COMPRESSED_DATA = new SyncResponse(Message.RESPONSE_CORRUPTED_COMPRESSED_DATA);
    public static final SyncResponse MALFORMED_JSON = new SyncResponse(Message.RESPONSE_MALFORMED_JSON);
    public static final SyncResponse VALID_RESPONSE = new SyncResponse(Message.RESPONSE_VALID_RESPONSE);
    public static final SyncResponse INVALID_CREDENTIALS = new SyncResponse(Message.RESPONSE_INVALID_CREDENTIALS);
    public static final SyncResponse PERMISSION_DENIED = new SyncResponse(Message.RESPONSE_PERMISSION_DENIED);
    public static final SyncResponse TOO_MANY_REQUESTS = new SyncResponse(Message.RESPONSE_TOO_MANY_REQUESTS);

    private final Message message;
    private final String messageString;

    public SyncResponse(Message message) {
      this(message, null);
    }

    public String getMessage(Platform platform) {
      return messageString != null ? messageString : message.toText(platform);
    }
  }
}
