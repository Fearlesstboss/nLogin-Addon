/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.Constants;
import com.nickuc.login.addon.core.util.security.SecureGenerator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Credentials {

  private @Getter String encryptionPassword;
  private @Getter @Nullable String linkedToken;
  private @Getter @Nullable String linkedEmail;
  private @Getter final Set<String> keys;
  private final Map<UUID, User> users;
  private volatile boolean modified;

  public User getUser(UUID id) {
    return users.computeIfAbsent(id, uuid -> new User(id, new HashMap<>()));
  }

  public void setEncryptionPassword(String encryptionPassword) {
    this.encryptionPassword = encryptionPassword;
    modified = true;
  }

  public void setLinkedToken(String linkedToken) {
    this.linkedToken = linkedToken;
    modified = true;
  }

  public void setLinkedEmail(String linkedEmail) {
    this.linkedEmail = linkedEmail;
    modified = true;
  }

  public void addKey(String key) {
    if (keys.add(key)) {
      modified = true;
    }
  }

  public String getMainKey() {
    if (keys.isEmpty() && keys.add(SecureGenerator.generateKey())) {
      modified = true;
    }
    return keys.stream().findFirst().get();
  }

  public boolean isModified() {
    if (modified) {
      return true;
    }

    for (User user : users.values()) {
      if (user.isModified()) {
        return modified = true;
      }
    }

    return false;
  }

  private void removeModified() {
    modified = false;
    for (User user : users.values()) {
      user.modified = false;
    }
  }

  public void save(File file) throws IOException  {
    if (isModified()) {
      System.out.println(Constants.PREFIX + "Saving credentials... " + file.getAbsolutePath());
      String json = Constants.GSON_PRETTY.toJson(serialize(new JsonObject()));
      @Cleanup PrintWriter writer = new PrintWriter(
          new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), false);
      writer.print(json);
      writer.flush();
      removeModified();
    }
  }

  public JsonObject serialize(JsonObject out) {
    out.addProperty("password", encryptionPassword);

    JsonObject linking = new JsonObject();
    if (linkedToken != null) {
      linking.addProperty("token", linkedToken);
      linking.addProperty("email", linkedEmail);
    }
    out.add("linking", linking);

    JsonArray keys = new JsonArray();
    for (String key : this.keys) {
      keys.add(key);
    }
    out.add("keys", keys);

    JsonArray usersJson = new JsonArray();
    for (User user : users.values()) {
      usersJson.add(user.serialize(new JsonObject()));
    }
    out.add("users", usersJson);

    return out;
  }

  public static Credentials deserialize(JsonObject in) {
    String encryptionPassword = in.has("password") ? in.get("password").getAsString().trim() : "";

    String linkedToken = null;
    String linkedEmail = null;
    if (in.has("linking")) {
      JsonObject linkingJson = in.getAsJsonObject("linking");
      if (linkingJson.has("token")) {
        linkedToken = linkingJson.get("token").getAsString();
        linkedEmail = linkingJson.get("email").getAsString();
      }
    }

    Set<String> keys = new LinkedHashSet<>();
    if (in.has("keys")) {
      JsonArray keysJson = in.getAsJsonArray("keys");
      for (int i = 0; i < keysJson.size(); i++) {
        keys.add(keysJson.get(i).getAsJsonPrimitive().getAsString());
      }
    }

    Map<UUID, User> users = new HashMap<>();
    if (in.has("users")) {
      JsonArray usersJson = in.getAsJsonArray("users");
      for (int i = 0; i < usersJson.size(); i++) {
        JsonObject userJson = usersJson.get(i).getAsJsonObject();
        User user = User.deserialize(userJson);
        if (user != null) {
          users.put(user.getId(), user);
        }
      }
    }

    return new Credentials(encryptionPassword, linkedToken, linkedEmail, keys, users, false);
  }
}
