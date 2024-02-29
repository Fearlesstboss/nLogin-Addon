package com.nickuc.login.addon.core.platform;

public interface Settings {

  boolean isEnabled();

  boolean isDebug();

  String getMainPassword();

  void setMainPassword(String mainPassword);

  boolean isSaveLogin();

  boolean isSyncPasswords();

}
