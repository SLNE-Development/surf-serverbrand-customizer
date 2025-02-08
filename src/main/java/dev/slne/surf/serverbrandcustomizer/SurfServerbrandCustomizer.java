package dev.slne.surf.serverbrandcustomizer;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Configuration.Server;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.netty.buffer.Unpooled;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.network.Utf8String;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class SurfServerbrandCustomizer extends JavaPlugin {

  private static final String BRAND_CHANNEL = "minecraft:brand";
  private static final DynamicCommandExceptionType CONFIG_RELOAD_FAILED = new DynamicCommandExceptionType(
      (e) -> new LiteralMessage("Failed to reload config: " + ((Throwable) e).getMessage()));

  private volatile String customServerBrand;

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void onLoad() {
    if (PacketEvents.getAPI() == null) {
      PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
      PacketEvents.getAPI().load();
    }

    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event ->
        event.registrar().register(
            Commands.literal("serverbrand")
                .then(
                    Commands.literal("reload")
                        .executes(context -> {
                          try {
                            reloadConfig();
                            loadFromConfig();
                          } catch (Throwable e) {
                            throw CONFIG_RELOAD_FAILED.create(e);
                          }
                          resendToOnlinePlayers();

                          context.getSource().getSender()
                              .sendMessage(Component.text("Reloaded custom server brand",
                                  NamedTextColor.GREEN));

                          return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("brand", StringArgumentType.greedyString())
                        .executes(context -> {
                          var brand = StringArgumentType.getString(context, "brand");
                          getConfig().set("brand", brand);
                          saveConfig();
                          loadFromConfig();
                          resendToOnlinePlayers();

                          context.getSource().getSender()
                              .sendMessage(
                                  Component.text("Set custom server brand", NamedTextColor.GREEN));

                          return Command.SINGLE_SUCCESS;
                        })))
                .build()
        )));
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadFromConfig();

    PacketEvents.getAPI().init();
    PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
      @Override
      public void onPacketSend(final PacketSendEvent event) {
        var state = event.getConnectionState();
        if (!state.equals(ConnectionState.PLAY) && !state.equals(ConnectionState.CONFIGURATION)) {
          return;
        }

        if (customServerBrand == null) {
          return;
        }

        var type = event.getPacketType();
        if (type.equals(Server.PLUGIN_MESSAGE)) {
          var packet = new WrapperConfigServerPluginMessage(event);
          if (!packet.getChannelName().equals(BRAND_CHANNEL)) {
            return;
          }

          packet.setData(getCustomServerBrandBytes());
        } else if (type.equals(Play.Server.PLUGIN_MESSAGE)) {
          var packet = new WrapperPlayServerPluginMessage(event);
          if (!packet.getChannelName().equals(BRAND_CHANNEL)) {
            return;
          }
          packet.setData(getCustomServerBrandBytes());
        }
      }
    });

    new Metrics(this, 	24696);
  }

  @Override
  public void onDisable() {
    PacketEvents.getAPI().terminate();
  }

  private byte @NotNull [] getCustomServerBrandBytes() {
    var buf = Unpooled.buffer();
    Utf8String.write(buf, customServerBrand, Short.MAX_VALUE);
    var data = new byte[buf.readableBytes()];
    buf.readBytes(data);

    return data;
  }

  private void resendToOnlinePlayers() {
    var packet = new WrapperPlayServerPluginMessage(BRAND_CHANNEL, getCustomServerBrandBytes());
    for (final Player player : Bukkit.getOnlinePlayers()) {
      PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
  }

  private void loadFromConfig() {
    var rawBrand = getConfig().getString("brand");

    if (rawBrand == null) {
      customServerBrand = null;
      return;
    }

    customServerBrand = LegacyComponentSerializer.legacySection()
                            .serialize(Component.text()
                                .append(MiniMessage.miniMessage().deserialize(rawBrand))
                                .build()
                            ) + "§r";
  }
}
