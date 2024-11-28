package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.MobEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PotionContentsComponent extends AbstractItemComponent<PotionContentsComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(@Nullable Integer potion, @Nullable Integer customColor, Collection<MobEffect> customEffects, @Nullable String customName) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      LimboProtocolUtils.writeOptional(buf, this.potion, ProtocolUtils::writeVarInt);
      LimboProtocolUtils.writeOptional(buf, this.customColor, ByteBuf::writeInt);
      LimboProtocolUtils.writeCollection(buf, this.customEffects, MobEffect::write);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        LimboProtocolUtils.writeOptional(buf, this.customName, ProtocolUtils::writeString);
      }
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(
          LimboProtocolUtils.readOptional(buf, ProtocolUtils::readVarInt),
          LimboProtocolUtils.readOptional(buf, ByteBuf::readInt),
          LimboProtocolUtils.readCollection(buf, MobEffect::read),
          version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? LimboProtocolUtils.readOptional(buf, ProtocolUtils::readString) : null
      );
    }
  }
}
