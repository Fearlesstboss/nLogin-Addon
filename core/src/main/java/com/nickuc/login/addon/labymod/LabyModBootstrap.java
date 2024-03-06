package com.nickuc.login.addon.labymod;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.core.Constants;
import com.nickuc.login.addon.core.handler.EventHandler;
import com.nickuc.login.addon.core.model.Credentials;
import com.nickuc.login.addon.core.nLoginAddon;
import com.nickuc.login.addon.core.packet.OutgoingPacket;
import com.nickuc.login.addon.core.platform.Platform;
import com.nickuc.login.addon.core.platform.Settings;
import com.nickuc.login.addon.labymod.LabyModBootstrap.Configuration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.labymod.api.Constants.Files;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.serializer.legacy.LegacyComponentSerializer;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ChatMessageSendEvent;
import net.labymod.api.event.client.network.server.NetworkPayloadEvent;
import net.labymod.api.event.client.network.server.NetworkPayloadEvent.Side;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import net.labymod.api.models.addon.annotation.AddonMain;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.I18n;
import net.labymod.api.util.MethodOrder;

@AddonMain
public class LabyModBootstrap extends LabyAddon<Configuration> implements Platform {

  private final nLoginAddon addon = new nLoginAddon(this);

  @Override
  protected void enable() {
    addon.enable();
    this.registerSettingCategory();

    try {
      NLOGIN_SETTINGS = labyAPI().coreSettingRegistry().getById(addonInfo().getNamespace());

      final Credentials credentials = addon.getCredentials();
      labyAPI().minecraft().executeOnRenderThread(() ->
          LabyModBootstrap.NLOGIN_SETTINGS.getById("unlinkAccount").asElement().setVisibleSupplier(() -> credentials.getLinkedToken() != null));
    } catch (Exception e) {
      error("Cannot modify addon settings behavior", e);
    }
  }

  @Override
  protected Class<Configuration> configurationClass() {
    return Configuration.class;
  }

  @Override
  public boolean isEnabled() {
    return configuration().enabled().get();
  }

  @Override
  public Settings getSettings() {
    return configuration();
  }

  @Override
  public Path getSettingsDirectory() {
    return Files.CONFIGS.resolve(addonInfo().getNamespace());
  }

  @Override
  public String translate(String key, Object... params) {
    return I18n.translate(addonInfo().getNamespace() + '.' + key, params);
  }

  @Override
  public void registerEvents(EventHandler handler) {
    super.registerListener(new EventListener(handler));
  }

  @Override
  public void sendRequest(OutgoingPacket outgoingPacket) {
    JsonObject out = new JsonObject();
    out.addProperty("id", outgoingPacket.id());

    JsonObject outData = new JsonObject();
    outgoingPacket.write(outData);
    out.add("data", outData);

    String data = out.toString();
    labyAPI().serverController().sendPayload(
        ResourceLocation.create(Constants.ADDON_MODERN_CHANNEL0, Constants.ADDON_MODERN_CHANNEL1),
        data.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void showNotification(String message) {
    Component content = LegacyComponentSerializer.legacySection().deserialize(message);
    Notification notification = Notification
        .builder()
        .title(Component.text("nLogin Addon", NamedTextColor.WHITE))
        .text(content)
        .build();
    labyAPI().notificationController().push(notification);
  }

  @Override
  public void info(String message) {
    logger().info(message);
  }

  @Override
  public void error(String message, Throwable t) {
    if (t != null) {
      logger().error(message, t);
    } else {
      logger().error(message);
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class EventListener {

    private final EventHandler eventHandler;

    @Subscribe
    public void onJoin(ServerJoinEvent event) {
      eventHandler.handleJoin();
    }

    @Subscribe
    public void onQuit(ServerDisconnectEvent event) {
      eventHandler.handleQuit();
    }

    @Subscribe
    public void onMessageSend(ChatMessageSendEvent event) {
      if (eventHandler.handleChat(event.getMessage())) {
        event.setCancelled(true);
      }
    }

    @Subscribe
    public void onPluginMessage(NetworkPayloadEvent event) {
      if (event.side() != Side.RECEIVE) {
        return;
      }

      ResourceLocation identifier = event.identifier();
      if (!Constants.ADDON_MODERN_CHANNEL0.equals(identifier.getNamespace()) || !Constants.ADDON_MODERN_CHANNEL1.equals(identifier.getPath())) {
        return;
      }

      eventHandler.handleCustomMessage(event.getPayload());
    }
  }

  public static Setting NLOGIN_SETTINGS;
  private static Runnable LINK_CALLBACK = () -> {}, UNLINK_CALLBACK = () -> {};

  @ConfigName("settings")
  @SpriteTexture(value = "settings")
  public static class Configuration extends AddonConfig implements Settings {

    @SwitchSetting
    @SpriteSlot(size = 32)
    private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

    @MethodOrder(after = "linkAccount")
    @SpriteSlot(size = 32, x = 1)
    @TextFieldSetting
    private final ConfigProperty<String> encryptionPassword = new ConfigProperty<>("");

    @SettingSection("general")
    @SpriteSlot(size = 32, y = 1)
    @SwitchSetting
    private final ConfigProperty<Boolean> saveLogin = new ConfigProperty<>(true);
    @SpriteSlot(size = 32, x = 3)
    @SwitchSetting
    private final ConfigProperty<Boolean> syncPasswords = new ConfigProperty<>(true);

    @SettingSection("developer")
    @SpriteSlot(size = 32, x = 2)
    @SwitchSetting
    private final ConfigProperty<Boolean> debug = new ConfigProperty<>(false);

    @SettingSection("backups")
    @MethodOrder(after = "enabled")
    @SpriteSlot(size = 32, x = 2, y = 1)
    @ButtonSetting
    public void linkAccount() {
      LINK_CALLBACK.run();
    }

    @MethodOrder(after = "linkAccount")
    @SpriteSlot(size = 32, x = 1, y = 1)
    @ButtonSetting
    public void unlinkAccount() {
      UNLINK_CALLBACK.run();
    }

    @Override
    public ConfigProperty<Boolean> enabled() {
      return enabled;
    }

    @Override
    public boolean isEnabled() {
      return enabled.get();
    }

    @Override
    public boolean isDebug() {
      return debug.get();
    }

    @Override
    public String getEncryptionPassword() {
      return encryptionPassword.get();
    }

    @Override
    public boolean isSaveLogin() {
      return saveLogin.get();
    }

    @Override
    public boolean isSyncPasswords() {
      return syncPasswords.get();
    }

    @Override
    public void init(String encryptionPassword, Runnable linkCallback, Runnable unlinkCallback) {
      this.encryptionPassword.set(encryptionPassword);
      LINK_CALLBACK = linkCallback;
      UNLINK_CALLBACK = unlinkCallback;
    }
  }
}
