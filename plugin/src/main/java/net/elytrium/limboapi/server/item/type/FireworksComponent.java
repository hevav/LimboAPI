package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class FireworksComponent extends AbstractItemComponent<FireworksComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(int flightDuration, Collection<FireworkExplosionComponent.Value> explosions) {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.flightDuration);
      LimboProtocolUtils.writeCollection(buf, this.explosions, 256, FireworkExplosionComponent.Value::write);
    }

    public static Value read(ByteBuf buf) {
      return new Value(ProtocolUtils.readVarInt(buf), LimboProtocolUtils.readCollection(buf, 256, FireworkExplosionComponent.Value::read));
    }
  }
}
