package com.nickuc.login.addon.core.model;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.util.security.RSA;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class Server {

  private final UUID id;
  @Nullable
  PublicKey key;
  String password;

  public JsonObject serialize(JsonObject out) {
    out.addProperty("id", id.toString());
    if (key != null) {
      out.addProperty("key", Base64.getEncoder().encodeToString(key.getEncoded()));
    }
    out.addProperty("password", password);
    return out;
  }

  @Nullable
  public static Server deserialize(JsonObject in) {
    try {
      UUID id = UUID.fromString(in.get("id").getAsString());
      String password = in.get("password").getAsString();
      PublicKey publicKey = in.has("key") ?
          RSA.getPublicKeyFromBase64(in.get("key").getAsString()) : null;
      return new Server(id, publicKey, password);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
