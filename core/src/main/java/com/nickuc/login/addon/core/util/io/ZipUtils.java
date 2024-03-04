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
