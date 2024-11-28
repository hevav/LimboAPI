package net.elytrium.limboapi.server.item.type.data;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;

public sealed interface ConsumeEffect permits ConsumeEffect.ApplyStatusEffects, ConsumeEffect.RemoveStatusEffects, ConsumeEffect.ClearAllStatusEffects,
    ConsumeEffect.TeleportRandomly, ConsumeEffect.PlaySound {

  void write(ByteBuf buf);

  static ConsumeEffect read(ByteBuf buf) {
    int id = ProtocolUtils.readVarInt(buf);
    return switch (id) {
      case 0 -> ApplyStatusEffects.read(buf);
      case 1 -> RemoveStatusEffects.read(buf);
      case 2 -> ClearAllStatusEffects.read();
      case 3 -> TeleportRandomly.read(buf);
      case 4 -> PlaySound.read(buf);
      default -> throw new IllegalStateException("Unexpected value: " + id);
    };
  }

  record ApplyStatusEffects(Collection<MobEffect> effects, float probability) implements ConsumeEffect {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, 0);
      LimboProtocolUtils.writeCollection(buf, this.effects, MobEffect::write);
      buf.writeFloat(this.probability);
    }

    static ConsumeEffect read(ByteBuf buf) {
      return new ApplyStatusEffects(LimboProtocolUtils.readCollection(buf, MobEffect::read), buf.readFloat());
    }
  }

  record RemoveStatusEffects(HolderSet effects) implements ConsumeEffect {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, 1);
      this.effects.write(buf);
    }

    static ConsumeEffect read(ByteBuf buf) {
      return new RemoveStatusEffects(HolderSet.read(buf));
    }
  }

  record ClearAllStatusEffects() implements ConsumeEffect {

    public static final ClearAllStatusEffects INSTANCE = new ClearAllStatusEffects();

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, 2);
    }

    static ConsumeEffect read() {
      return ClearAllStatusEffects.INSTANCE;
    }
  }

  record TeleportRandomly(float diameter) implements ConsumeEffect {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, 3);
      buf.writeFloat(this.diameter);
    }

    static ConsumeEffect read(ByteBuf buf) {
      return new TeleportRandomly(buf.readFloat());
    }
  }

  record PlaySound(SoundEvent sound) implements ConsumeEffect {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, 4);
      this.sound.write(buf);
    }

    static ConsumeEffect read(ByteBuf buf) {
      return new PlaySound(SoundEvent.read(buf));
    }
  }
}
