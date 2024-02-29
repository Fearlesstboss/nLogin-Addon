package com.nickuc.login.addon.core.manager;

import com.nickuc.login.addon.core.model.Session;
import com.nickuc.login.addon.core.util.SecureGenerator;
import org.jetbrains.annotations.Nullable;

public class SessionManager {

  private volatile Session current;

  public Session newSession() {
    return current = new Session(SecureGenerator.generateRSAChallenge());
  }

  public void invalidate() {
    this.current = null;
  }

  @Nullable
  public Session getCurrent() {
    return current;
  }
}
