package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.SoundEvent;
import net.elytrium.limboapi.server.item.type.data.VersionLessComponentHolder;

public class InstrumentComponent extends AbstractItemComponent<InstrumentComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public sealed interface Value permits Value.ReferenceValue, Value.DirectValue {

    void write(ByteBuf buf, ProtocolVersion version);

    static Value read(ByteBuf buf, ProtocolVersion version) {
      int i = ProtocolUtils.readVarInt(buf);
      return i == 0 ? DirectValue.read(buf, version) : new ReferenceValue(i - 1);
    }

    record ReferenceValue(int id) implements Value {

      @Override
      public void write(ByteBuf buf, ProtocolVersion version) {
        ProtocolUtils.writeVarInt(buf, this.id + 1);
      }
    }

    record DirectValue(SoundEvent soundEvent, int useDuration, float range, VersionLessComponentHolder description) implements Value {

      @Override
      public void write(ByteBuf buf, ProtocolVersion version) {
        this.soundEvent.write(buf);
        ProtocolUtils.writeVarInt(buf, this.useDuration);
        buf.writeFloat(this.range);
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
          if (this.description == null) {
            throw new IllegalArgumentException("description cannot be null on >=1.21.2!");
          }

          this.description.write(buf, version);
        }
      }

      public static DirectValue read(ByteBuf buf, ProtocolVersion version) {
        return new DirectValue(
            SoundEvent.read(buf),
            ProtocolUtils.readVarInt(buf),
            buf.readFloat(),
            version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? VersionLessComponentHolder.read(buf, version) : null
        );
      }
    }
  }
}
