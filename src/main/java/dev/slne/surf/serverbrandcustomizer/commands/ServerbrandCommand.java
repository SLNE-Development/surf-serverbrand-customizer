package dev.slne.surf.serverbrandcustomizer.commands;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.slne.surf.serverbrandcustomizer.SurfServerbrandCustomizer;
import dev.slne.surf.serverbrandcustomizer.config.ServerbrandConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class ServerbrandCommand {

  private static final DynamicCommandExceptionType CONFIG_RELOAD_FAILED = new DynamicCommandExceptionType(
      (e) -> new LiteralMessage("Failed to reload config: " + ((Throwable) e).getMessage()));

  public static void register(Commands commands) {
    commands.register(Commands.literal("serverbrand")
        .then(Commands.literal("reload")
            .executes(context -> doReload(context.getSource())))
        .then(Commands.literal("set")
            .then(Commands.argument("brand", StringArgumentType.greedyString())
                .executes(context -> doSetBrand(context.getSource(),
                    StringArgumentType.getString(context, "brand")))))
        .build());
  }

  private static int doReload(CommandSourceStack source) throws CommandSyntaxException {
    var plugin = SurfServerbrandCustomizer.getInstance();
    try {
      plugin.reload();
    } catch (Throwable e) {
      throw CONFIG_RELOAD_FAILED.create(e);
    }
    resendToOnlinePlayers(plugin.getServerbrandConfig());

    source.getSender()
        .sendMessage(Component.text("Reloaded custom server brand",
            NamedTextColor.GREEN));

    return Command.SINGLE_SUCCESS;
  }

  private static int doSetBrand(CommandSourceStack source, String brand) {
    var config = SurfServerbrandCustomizer.getInstance().getServerbrandConfig();

    config.setCustomServerBrand(brand);
    resendToOnlinePlayers(config);

    source.getSender()
        .sendMessage(
            Component.text("Set custom server brand", NamedTextColor.GREEN));

    return Command.SINGLE_SUCCESS;
  }

  private static void resendToOnlinePlayers(ServerbrandConfig config) {
    var packet = new WrapperPlayServerPluginMessage(SurfServerbrandCustomizer.BRAND_CHANNEL,
        config.getCustomServerBrandBytes());
    for (final Player player : Bukkit.getOnlinePlayers()) {
      PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
  }
}
