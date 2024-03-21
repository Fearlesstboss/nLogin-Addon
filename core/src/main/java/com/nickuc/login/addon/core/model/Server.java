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
