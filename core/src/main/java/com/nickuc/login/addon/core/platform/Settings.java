package com.nickuc.login.addon.core.platform;

public interface Settings {

  boolean isEnabled();

  boolean isDebug();

  String getEncryptionPassword();

  boolean isSaveLogin();

  boolean isSyncPasswords();

  void init(String encryptionPassword, Runnable linkCallback, Runnable unlinkCallback);

}