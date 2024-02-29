package com.nickuc.login.addon.core.util;

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
    while ((size = input.read(buf, 0, bufSize)) != -1) {
      output.write(buf, 0, size);
    }
    output.flush();
  }
}
