/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.core.packet;

import com.google.gson.JsonObject;

public interface IncomingPacket {

  void read(JsonObject in);

}
