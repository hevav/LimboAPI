package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class SuspiciousStewEffectsComponent extends AbstractItemComponent<Collection<SuspiciousStewEffectsComponent.Entry>> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readCollection(buf, Entry::read));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeCollection(buf, this.getValue(), Entry::write);
  }

  public record Entry(int effect, int duration) {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.effect);
      ProtocolUtils.writeVarInt(buf, this.duration);
    }

    public static Entry read(ByteBuf buf) {
      return new Entry(ProtocolUtils.readVarInt(buf), ProtocolUtils.readVarInt(buf));
    }
  }
}
