package dev.slne.surf.serverbrandcustomizer;

import com.github.retrooper.packetevents.PacketEvents;
import dev.slne.surf.serverbrandcustomizer.commands.ServerbrandCommand;
import dev.slne.surf.serverbrandcustomizer.config.ServerbrandConfig;
import dev.slne.surf.serverbrandcustomizer.listener.BrandSendListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class SurfServerbrandCustomizer extends JavaPlugin {

  public static final String BRAND_CHANNEL = "minecraft:brand";
  private final ServerbrandConfig serverbrandConfig = new ServerbrandConfig(this);

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void onLoad() {
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
      var commands = event.registrar();
      ServerbrandCommand.register(commands);
    }));
  }

  @Override
  public void onEnable() {
    serverbrandConfig.load();

    PacketEvents.getAPI().getEventManager()
        .registerListener(new BrandSendListener(serverbrandConfig));

    new Metrics(this, 24696);
  }

  public void reload() {
    serverbrandConfig.reload();
  }

  public ServerbrandConfig getServerbrandConfig() {
    return serverbrandConfig;
  }

  public static @NotNull SurfServerbrandCustomizer getInstance() {
    return getPlugin(SurfServerbrandCustomizer.class);
  }
}
