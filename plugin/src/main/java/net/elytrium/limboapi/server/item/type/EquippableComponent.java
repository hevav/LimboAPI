package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.HolderSet;
import net.elytrium.limboapi.server.item.type.data.SoundEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EquippableComponent extends AbstractItemComponent<EquippableComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(int slot, SoundEvent equipSound, @Nullable String model, @Nullable String cameraOverlay, @Nullable HolderSet allowedEntities,
      boolean dispensable, boolean swappable, boolean damageOnHurt) {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.slot);
      this.equipSound.write(buf);
      LimboProtocolUtils.writeOptional(buf, this.model, ProtocolUtils::writeString);
      LimboProtocolUtils.writeOptional(buf, this.cameraOverlay, ProtocolUtils::writeString);
      LimboProtocolUtils.writeOptional(buf, this.allowedEntities, HolderSet::write);
      buf.writeBoolean(this.dispensable);
      buf.writeBoolean(this.swappable);
      buf.writeBoolean(this.damageOnHurt);
    }

    public static Value read(ByteBuf buf) {
      return new Value(
          ProtocolUtils.readVarInt(buf),
          SoundEvent.read(buf),
          LimboProtocolUtils.readOptional(buf, ProtocolUtils::readString),
          LimboProtocolUtils.readOptional(buf, ProtocolUtils::readString),
          LimboProtocolUtils.readOptional(buf, HolderSet::read),
          buf.readBoolean(),
          buf.readBoolean(),
          buf.readBoolean()
      );
    }
  }
}
