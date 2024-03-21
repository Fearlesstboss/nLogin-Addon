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
  private final Map<String, Server> servers;
  @Getter volatile boolean modified;

  public Server updateServer(String id, PublicKey key, String password) {
    Server server = getServer(id);
    if (server != null) {
      server.password = password;
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
  public Server getServer(String id) {
    return servers.get(id);
  }

  @Nullable
  public JsonObject serialize(JsonObject out) {
    out.addProperty("id", id.toString());

    JsonArray serversJson = new JsonArray();
    for (Server server : servers.values()) {
      serversJson.add(server.serialize(new JsonObject()));
    }
    out.add("servers", serversJson);

    return out;
  }

  @Nullable
  static User deserialize(JsonObject in) {
    try {
      JsonArray serversJson = in.getAsJsonArray("servers");

      Map<String, Server> servers = new HashMap<>();
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
