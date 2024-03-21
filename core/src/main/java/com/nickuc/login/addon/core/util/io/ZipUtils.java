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
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class ZipUtils {

  public static byte[] compress(byte[] in) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try (DeflaterOutputStream deflate = new DeflaterOutputStream(outputStream)) {
        deflate.write(in);
      }
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Could not compress with Gzip!", e);
    }
  }

  public static byte[] decompress(byte[] in) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try (InflaterOutputStream inflate = new InflaterOutputStream(outputStream)) {
        inflate.write(in);
        inflate.flush();
      }
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Could not decompress with Gzip!", e);
    }
  }
}
