package com.nickuc.login.addon.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nickuc.login.addon.core.util.SecureGenerator;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class User {

  private @Getter final UUID id;
  private @Getter final Set<String> mainKeys;
  private final Map<UUID, Server> servers;
  @Getter boolean modified;

  User(UUID id, Set<String> mainKeys, Map<UUID, Server> servers) {
    this.id = id;
    this.servers = servers;
    this.mainKeys = mainKeys;
    if (mainKeys.isEmpty()) {
      addMainKey(SecureGenerator.generateMainKey());
    }
  }

  public String getMainKey() {
    if (mainKeys.isEmpty()) {
      addMainKey(SecureGenerator.generateMainKey());
    }
    return mainKeys.stream().findFirst().get();
  }

  public void addMainKey(String key) {
    if (mainKeys.add(key)) {
      modified = true;
    }
  }

  public Server updateServer(UUID id, PublicKey key, String password) {
    Server server = getServer(id);
    if (server != null) {
      server.password = password;
      server.key = key;
    } else {
      servers.put(id, server = new Server(id, key, password));
    }
    modified = true;
    return server;
  }

  public void updateServer(Server server) {
    servers.put(server.getId(), server);
    modified = true;
  }

  @Nullable
  public Server getServer(UUID id) {
    return servers.get(id);
  }

  @Nullable
  public JsonObject serialize(JsonObject out) {
    out.addProperty("id", id.toString());

    JsonArray mainKeys = new JsonArray();
    for (String mainKey : this.mainKeys) {
      mainKeys.add(new JsonPrimitive(mainKey));
    }
    out.add("main-keys", mainKeys);

    JsonArray serversJson = new JsonArray();
    for (Map.Entry<UUID, Server> entry : servers.entrySet()) {
      Server server = entry.getValue();
      serversJson.add(server.serialize(new JsonObject()));
    }
    out.add("servers", serversJson);

    return out;
  }

  @Nullable
  static User deserialize(JsonObject in) {
    try {
      JsonArray mainKeyJson = in.getAsJsonArray("main-keys");
      JsonArray serversJson = in.getAsJsonArray("servers");

      Set<String> mainKeys = new LinkedHashSet<>();
      for (int i = 0; i < mainKeyJson.size(); i++) {
        mainKeys.add(mainKeyJson.get(i).getAsJsonPrimitive().getAsString());
      }

      Map<UUID, Server> servers = new HashMap<>();
      for (int i = 0; i < serversJson.size(); i++) {
        JsonObject serverJson = serversJson.get(i).getAsJsonObject();
        Server server = Server.deserialize(serverJson);
        if (server != null) {
          servers.put(server.getId(), server);
        }
      }

      return new User(UUID.fromString(in.get("id").getAsString()), mainKeys, servers);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
