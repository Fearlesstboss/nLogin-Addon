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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;

@AllArgsConstructor
public class Credentials {

  private @Getter final UUID id;
  private @Getter String mainPassword;
  private final Map<UUID, User> users;
  private boolean modified;

  public User getUser(UUID id) {
    return users.computeIfAbsent(id, uuid ->
        new User(id, new LinkedHashSet<>(), new HashMap<>()));
  }

  public void setMainPassword(String mainPassword) {
    this.mainPassword = mainPassword;
    modified = true;
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
      @Cleanup PrintWriter writer = new PrintWriter(
          new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), false);
      writer.print(Constants.GSON_PRETTY.toJson(serialize(new JsonObject())));
      writer.flush();
      removeModified();
    }
  }

  public JsonObject serialize(JsonObject out) {
    out.addProperty("id", id.toString());
    out.addProperty("main-password", mainPassword);

    JsonArray usersJson = new JsonArray();
    for (User user : users.values()) {
      usersJson.add(user.serialize(new JsonObject()));
    }
    out.add("users", usersJson);

    return out;
  }

  public static Credentials deserialize(JsonObject in) {
    UUID id = in.has("id") ? UUID.fromString(in.get("id").getAsString()) : UUID.randomUUID();
    String mainPassword = in.has("main-password") ? in.get("main-password").getAsString().trim() : "";

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

    return new Credentials(id, mainPassword, users, false);
  }
}
