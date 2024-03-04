package com.nickuc.login.addon.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class User {

  private @Getter final UUID id;
  private final Map<UUID, Server> servers;
  @Getter boolean modified;

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
      JsonArray serversJson = in.getAsJsonArray("servers");

      Map<UUID, Server> servers = new HashMap<>();
      for (int i = 0; i < serversJson.size(); i++) {
        JsonObject serverJson = serversJson.get(i).getAsJsonObject();
        Server server = Server.deserialize(serverJson);
        if (server != null) {
          servers.put(server.getId(), server);
        }
      }

      return new User(UUID.fromString(in.get("id").getAsString()), servers);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
