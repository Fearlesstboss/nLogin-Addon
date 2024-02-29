package com.nickuc.login.addon.core.packet;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.handler.PacketHandler;
import com.nickuc.login.addon.core.packet.incoming.IncomingReadyPacket;
import com.nickuc.login.addon.core.packet.incoming.IncomingServerStatusPacket;
import com.nickuc.login.addon.core.packet.incoming.IncomingSyncDataPacket;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class PacketRegistry {

  private final Map<Integer, Class<IncomingPacket>> id = new HashMap<>();
  private final Map<Integer, Consumer<IncomingPacket>> handler = new HashMap<>();

  public PacketRegistry(PacketHandler packetHandler) {
    register(0, IncomingReadyPacket.class, packetHandler::handleReady);
    register(1, IncomingSyncDataPacket.class, packetHandler::handleSync);
    register(2, IncomingServerStatusPacket.class, packetHandler::handleServerStatus);
  }

  @SuppressWarnings("unchecked")
  private <T extends IncomingPacket> void register(int id, Class<T> clazz, Consumer<T> handler) {
    this.id.put(id, (Class<IncomingPacket>) clazz);
    this.handler.put(id, (Consumer<IncomingPacket>) handler);
  }

  @Nullable
  public IncomingPacket create(JsonObject json, int id) {
    Class<? extends IncomingPacket> clazz = this.id.get(id);
    if (clazz == null) {
      return null;
    }

    IncomingPacket incomingPacket;
    try {
      incomingPacket = clazz.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

      return incomingPacket;
  }

  public <T extends IncomingPacket> boolean handle(T decodePacket, int id) {
    Consumer<IncomingPacket> handler = this.handler.get(id);
    if (handler != null) {
      handler.accept(decodePacket);
      return true;
    } else return false;
  }
}
