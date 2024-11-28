package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class BeesComponent extends AbstractItemComponent<Collection<BeesComponent.Occupant>> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readCollection(buf, () -> Occupant.read(buf, version)));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeCollection(buf, this.getValue(), occupant -> occupant.write(buf, version));
  }

  public record Occupant(CompoundBinaryTag entityData, int ticksInHive, int minTicksInHive) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      ProtocolUtils.writeBinaryTag(buf, version, this.entityData);
      ProtocolUtils.writeVarInt(buf, this.ticksInHive);
      ProtocolUtils.writeVarInt(buf, this.minTicksInHive);
    }

    public static Occupant read(ByteBuf buf, ProtocolVersion version) {
      return new Occupant(ProtocolUtils.readCompoundTag(buf, version, null), ProtocolUtils.readVarInt(buf), ProtocolUtils.readVarInt(buf));
    }
  }
}
