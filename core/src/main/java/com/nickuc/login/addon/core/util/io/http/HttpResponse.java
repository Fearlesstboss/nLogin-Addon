/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.util.io.http;

import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Getter
@Accessors(chain = true, fluent = true)
@ToString
public class HttpResponse {

  private final byte[] content;
  private final int responseCode;
  private final Throwable throwable;

  public HttpResponse(byte[] content, int responseCode) {
    this(content, responseCode, null);
  }

  public String contentUTF8() {
    if (content == null) {
      return null;
    }

    return new String(content, StandardCharsets.UTF_8);
  }
}
