package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.ConsumeEffect;
import net.elytrium.limboapi.server.item.type.data.SoundEvent;

public class ConsumableComponent extends AbstractItemComponent<ConsumableComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(float consumeSeconds, int animation, SoundEvent sound, boolean hasConsumeParticles, Collection<ConsumeEffect> onConsumeEffects) {

    public void write(ByteBuf buf) {
      buf.writeFloat(this.consumeSeconds);
      ProtocolUtils.writeVarInt(buf, this.animation);
      this.sound.write(buf);
      buf.writeBoolean(this.hasConsumeParticles);
      LimboProtocolUtils.writeCollection(buf, this.onConsumeEffects, ConsumeEffect::write);
    }

    public static Value read(ByteBuf buf) {
      return new Value(buf.readFloat(), ProtocolUtils.readVarInt(buf), SoundEvent.read(buf), buf.readBoolean(), LimboProtocolUtils.readCollection(buf, ConsumeEffect::read));
    }
  }
}
