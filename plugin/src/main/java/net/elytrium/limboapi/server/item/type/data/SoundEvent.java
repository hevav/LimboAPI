package net.elytrium.limboapi.server.item.type.data;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface SoundEvent permits SoundEvent.ReferenceSoundEvent, SoundEvent.DirectSoundEvent {

  void write(ByteBuf buf);

  static SoundEvent read(ByteBuf buf) {
    int i = ProtocolUtils.readVarInt(buf);
    return i == 0 ? DirectSoundEvent.read(buf) : new ReferenceSoundEvent(i - 1);
  }

  record DirectSoundEvent(String soundId, @Nullable Float range) implements SoundEvent {

    @Override
    public void write(ByteBuf buf) {
      ProtocolUtils.writeString(buf, this.soundId);
      LimboProtocolUtils.writeOptional(buf, this.range, ByteBuf::writeFloat);
    }

    public static DirectSoundEvent read(ByteBuf buf) {
      return new DirectSoundEvent(ProtocolUtils.readString(buf), LimboProtocolUtils.readOptional(buf, ByteBuf::readFloat));
    }
  }

  record ReferenceSoundEvent(int id) implements SoundEvent {

    @Override
    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.id + 1);
    }
  }
}
