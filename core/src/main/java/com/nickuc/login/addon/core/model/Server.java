package com.nickuc.login.addon.core.model;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.util.security.RSA;
import com.nickuc.login.addon.core.util.security.SHA256;
import java.security.PublicKey;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class Server {

  private final String id;
  private final PublicKey key;
  String password;

  public JsonObject serialize(JsonObject out) {
    out.addProperty("key", Base64.getEncoder().encodeToString(key.getEncoded()));
    out.addProperty("password", password);
    return out;
  }

  @Nullable
  public static Server deserialize(JsonObject in) {
    try {
      byte[] encodedKey = Base64.getDecoder().decode(in.get("key").getAsString());
      String id = SHA256.hash(encodedKey);
      String password = in.get("password").getAsString();
      PublicKey key = RSA.getPublicKeyFromBytes(encodedKey);
      return new Server(id, key, password);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
