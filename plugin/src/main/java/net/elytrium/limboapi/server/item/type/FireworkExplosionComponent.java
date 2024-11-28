package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FireworkExplosionComponent extends AbstractItemComponent<FireworkExplosionComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(int shape, int @Nullable [] colors, int @Nullable [] fadeColors, boolean hasTrail, boolean hasTwinkle) {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.shape);
      LimboProtocolUtils.writeIntArray(buf, this.colors);
      LimboProtocolUtils.writeIntArray(buf, this.fadeColors);
      buf.writeBoolean(this.hasTrail);
      buf.writeBoolean(this.hasTwinkle);
    }

    public static Value read(ByteBuf buf) {
      return new Value(ProtocolUtils.readVarInt(buf), LimboProtocolUtils.readIntArray(buf), LimboProtocolUtils.readIntArray(buf), buf.readBoolean(), buf.readBoolean());
    }
  }
}
