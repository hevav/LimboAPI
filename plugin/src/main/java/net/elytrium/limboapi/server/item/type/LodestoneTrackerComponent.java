package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.packets.data.GlobalPos;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LodestoneTrackerComponent extends AbstractItemComponent<LodestoneTrackerComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(@Nullable GlobalPos target, boolean tracked) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      LimboProtocolUtils.writeOptional(buf, this.target, value -> LimboProtocolUtils.writeGlobalPos(buf, version, value));
      buf.writeBoolean(this.tracked);
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(LimboProtocolUtils.readOptional(buf, () -> LimboProtocolUtils.readGlobalPos(buf, version)), buf.readBoolean());
    }
  }
}
