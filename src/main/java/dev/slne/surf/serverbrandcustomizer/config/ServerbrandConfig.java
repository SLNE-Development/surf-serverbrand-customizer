package dev.slne.surf.serverbrandcustomizer.config;

import dev.slne.surf.serverbrandcustomizer.SurfServerbrandCustomizer;
import dev.slne.surf.serverbrandcustomizer.buf.Utf8String;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public final class ServerbrandConfig {

  private final SurfServerbrandCustomizer plugin;
  private volatile String customServerBrand;

  public ServerbrandConfig(SurfServerbrandCustomizer plugin) {
    this.plugin = plugin;
  }

  public void load() {
    plugin.saveDefaultConfig();
    reloadFromConfig();
  }

  public void reload() {
    plugin.reloadConfig();
    reloadFromConfig();
  }

  public void reloadFromConfig() {
    var rawBrand = plugin.getConfig().getString("brand");

    if (rawBrand == null) {
      customServerBrand = null;
      return;
    }

    //noinspection deprecation
    customServerBrand = LegacyComponentSerializer.legacySection()
                            .serialize(Component.text()
                                .append(MiniMessage.miniMessage().deserialize(rawBrand))
                                .build()
                            ) + ChatColor.RESET;
  }

  public boolean isCustomServerBrandSet() {
    return customServerBrand != null;
  }

  public void setCustomServerBrand(String customServerBrand) {
    plugin.getConfig().set("brand", customServerBrand);
    plugin.saveConfig();
    reloadFromConfig();
  }

  public byte @NotNull [] getCustomServerBrandBytes() {
    var buf = Unpooled.buffer();
    Utf8String.writeString(buf, customServerBrand);
    var data = new byte[buf.readableBytes()];
    buf.readBytes(data);

    return data;
  }
}
