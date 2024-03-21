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

package com.nickuc.login.addon.core.packet.incoming;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.packet.IncomingPacket;
import com.nickuc.login.addon.core.util.security.RSA;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class IncomingReadyPacket implements IncomingPacket {

  private UUID userId;
  private int maxAllowedData;

  private boolean userRegistered;
  private boolean requireSync;

  private PublicKey key;
  private byte[] signature;

  @Override
  public void read(JsonObject in) {
    userId = UUID.fromString(in.get("user-id").getAsString());
    maxAllowedData = in.get("max-allowed-data").getAsInt();

    JsonObject clientJson = in.getAsJsonObject("client");
    userRegistered = clientJson.get("user-registered").getAsBoolean();
    requireSync = userRegistered && clientJson.get("require-sync").getAsBoolean();

    JsonObject challengeJson = in.get("challenge").getAsJsonObject();
    key = RSA.getPublicKeyFromBase64(challengeJson.get("key").getAsString());
    signature = Base64.getDecoder().decode(challengeJson.get("signature").getAsString());
  }
}
