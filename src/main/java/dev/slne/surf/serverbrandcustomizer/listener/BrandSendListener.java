package dev.slne.surf.serverbrandcustomizer.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Configuration;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import dev.slne.surf.serverbrandcustomizer.SurfServerbrandCustomizer;
import dev.slne.surf.serverbrandcustomizer.config.ServerbrandConfig;

public final class BrandSendListener extends PacketListenerAbstract {

  private final ServerbrandConfig config;

  public BrandSendListener(ServerbrandConfig config) {
    this.config = config;
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    var state = event.getConnectionState();
    if (!state.equals(ConnectionState.PLAY) && !state.equals(ConnectionState.CONFIGURATION)) {
      return;
    }

    if (!config.isCustomServerBrandSet()) {
      return;
    }

    var type = event.getPacketType();
    if (type.equals(Configuration.Server.PLUGIN_MESSAGE)) {
      var packet = new WrapperConfigServerPluginMessage(event);
      if (!packet.getChannelName().equals(SurfServerbrandCustomizer.BRAND_CHANNEL)) {
        return;
      }

      packet.setData(config.getCustomServerBrandBytes());
      event.markForReEncode(true);
    } else if (type.equals(Play.Server.PLUGIN_MESSAGE)) {
      var packet = new WrapperPlayServerPluginMessage(event);
      if (!packet.getChannelName().equals(SurfServerbrandCustomizer.BRAND_CHANNEL)) {
        return;
      }

      packet.setData(config.getCustomServerBrandBytes());
      event.markForReEncode(true);
    }
  }
}
