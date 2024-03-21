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

package com.nickuc.login.addon.core.util.io.http;

import com.nickuc.login.addon.core.util.io.IOUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Accessors(fluent = true)
public class HttpClient {

  private static final String USER_AGENT = "nLogin Addon (+https://github.com/nickuc-com/nLogin-Addon)";
  private static final int MAX_REDIRECTS = 20;

  private final Map<String, String> requestProperties = new HashMap<>();

  private @Setter int connectTimeout = 7500;
  private @Setter int readTimeout = connectTimeout * 3;
  private int redirectLoop;

  public static HttpClient create() {
    return new HttpClient();
  }

  public HttpClient header(String name, Object value) {
    requestProperties.put(name, value != null ? value.toString() : "null");
    return this;
  }

  public HttpClient token(String token) {
    return header("Authorization", "Bearer " + token);
  }

  private void buildHttp(HttpURLConnection http, String requestMethod)
      throws IllegalArgumentException {
    http.setInstanceFollowRedirects(false);
    if (requestMethod != null && !requestMethod.isEmpty()) {
      try {
        http.setRequestMethod(requestMethod);
      } catch (ProtocolException e) {
        throw new IllegalArgumentException("HTTP method \"" + requestMethod + "\" does not exists!",
            e);
      }
    }

    requestProperties.putIfAbsent("User-Agent", USER_AGENT);
    requestProperties.forEach(http::setRequestProperty);
    http.setConnectTimeout(connectTimeout);
    http.setReadTimeout(readTimeout);
  }

  private HttpResponse readRemoteResponse(HttpURLConnection http) throws IOException {
    redirectLoop = 0;

    int responseCode = http.getResponseCode();
    try {
      @Cleanup InputStream is = http.getErrorStream();
      if (is == null) {
        is = http.getInputStream();
      }

      byte[] contentBytes = IOUtil.readAll(is);
      return new HttpResponse(contentBytes, responseCode);
    } catch (IOException e) {
      return new HttpResponse(null, responseCode, e);
    }
  }

  public HttpResponse get(String url) {
    try {
      return getThrowing(url);
    } catch (IOException e) {
      return new HttpResponse(null, 0, e);
    }
  }

  public HttpResponse getThrowing(String url) throws IOException {
    HttpURLConnection http = null;
    try {
      http = (HttpURLConnection) new URL(url).openConnection();
      buildHttp(http, "GET");

      boolean redirect;
      do {
        int responseCode = http.getResponseCode();
        redirect = shouldRedirect(responseCode) && redirectLoop++ < MAX_REDIRECTS;

        if (redirect) {
          http.disconnect();

          String newUrl = http.getHeaderField("Location");
          http = (HttpURLConnection) new URL(newUrl).openConnection();
          buildHttp(http, "GET");
        }

      } while (redirect);

      return readRemoteResponse(http);
    } catch (SocketTimeoutException e) {
      throw new SocketTimeoutException("[GET] The connection took too long to be answered.");
    } finally {
      if (http != null) {
        http.disconnect();
      }
    }
  }

  public HttpResponse post(String url, byte[] data) {
    try {
      return postThrowing(url, data);
    } catch (IOException e) {
      return new HttpResponse(null, 0, e);
    }
  }

  public HttpResponse postThrowing(String url, byte[] data) throws IOException {
    HttpURLConnection http = null;
    try {
      requestProperties.putIfAbsent("Connection", "close");
      requestProperties.putIfAbsent("Content-Length", Integer.toString(data.length));

      http = (HttpURLConnection) new URL(url).openConnection();
      buildHttp(http, "POST");

      http.setDoOutput(true);
      @Cleanup DataOutputStream firstDos = new DataOutputStream(http.getOutputStream());
      firstDos.write(data);
      firstDos.flush();

      boolean redirect;
      do {
        int responseCode = http.getResponseCode();
        redirect = shouldRedirect(responseCode) && redirectLoop++ < MAX_REDIRECTS;

        if (redirect) {
          http.disconnect();

          String newUrl = http.getHeaderField("Location");
          http = (HttpURLConnection) new URL(newUrl).openConnection();
          buildHttp(http, null);

          http.setDoOutput(true);
          @Cleanup DataOutputStream newDos = new DataOutputStream(http.getOutputStream());
          newDos.write(data);
          newDos.flush();
        }

      } while (redirect);

      return readRemoteResponse(http);
    } catch (SocketTimeoutException e) {
      throw new SocketTimeoutException("[POST] The connection took too long to be answered.");
    } finally {
      if (http != null) {
        http.disconnect();
      }
    }
  }

  private boolean shouldRedirect(int httpCode) {
    return httpCode == HttpURLConnection.HTTP_MOVED_TEMP
        || httpCode == HttpURLConnection.HTTP_MOVED_PERM
        || httpCode == HttpURLConnection.HTTP_SEE_OTHER;
  }
}
