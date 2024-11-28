package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UseCooldownComponent extends AbstractItemComponent<UseCooldownComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(float seconds, @Nullable String cooldownGroup) {

    public void write(ByteBuf buf) {
      buf.writeFloat(this.seconds);
      LimboProtocolUtils.writeOptional(buf, this.cooldownGroup, ProtocolUtils::writeString);
    }

    public static Value read(ByteBuf buf) {
      return new Value(buf.readFloat(), LimboProtocolUtils.readOptional(buf, ProtocolUtils::readString));
    }
  }
}
