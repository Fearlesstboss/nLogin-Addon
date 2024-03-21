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

package com.nickuc.login.addon.core.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.Cleanup;
import org.jetbrains.annotations.CheckReturnValue;

public class IOUtil {

  private static final int BUFFER_SIZE = 4096;

  @CheckReturnValue
  public static byte[] readAll(InputStream input) throws IOException {
    return readAll(input, BUFFER_SIZE);
  }

  @CheckReturnValue
  public static byte[] readAll(InputStream input, int bufSize) throws IOException {
    @Cleanup ByteArrayOutputStream buf = new ByteArrayOutputStream();
    write(input, buf, bufSize);
    return buf.toByteArray();
  }

  public static void write(InputStream input, OutputStream output, int bufSize) throws IOException {
    byte[] buf = new byte[bufSize];
    int size;
    while (input.available() > 0 && (size = input.read(buf, 0, bufSize)) != -1) {
      output.write(buf, 0, size);
    }
    output.flush();
  }
}
