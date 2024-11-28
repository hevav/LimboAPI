package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.api.protocol.packets.data.ItemStack;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.MobEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FoodPropertiesComponent extends AbstractItemComponent<FoodPropertiesComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, @Nullable ItemStack usingConvertsTo, @Nullable Collection<PossibleEffect> effects) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      ProtocolUtils.writeVarInt(buf, this.nutrition);
      buf.writeFloat(this.saturation);
      buf.writeBoolean(this.canAlwaysEat);
      boolean lt1_21_2 = version.lessThan(ProtocolVersion.MINECRAFT_1_21_2);
      if (lt1_21_2) {
        buf.writeFloat(this.eatSeconds);
      }
      if (version == ProtocolVersion.MINECRAFT_1_21) {
        LimboProtocolUtils.writeOptional(buf, this.usingConvertsTo, itemStack -> LimboProtocolUtils.writeItemStack(buf, version, itemStack, false));
      }
      if (lt1_21_2) {
        LimboProtocolUtils.writeCollection(buf, this.effects, PossibleEffect::write);
      }
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      boolean lt1_21_2 = version.lessThan(ProtocolVersion.MINECRAFT_1_21_2);
      return new Value(
          ProtocolUtils.readVarInt(buf),
          buf.readFloat(),
          buf.readBoolean(),
          lt1_21_2 ? buf.readFloat() : 1.6F/*default value, source: ViaVersion*/,
          version == ProtocolVersion.MINECRAFT_1_21 ? LimboProtocolUtils.readOptional(buf, () -> LimboProtocolUtils.readItemStack(buf, version, false)) : null,
          lt1_21_2 ? LimboProtocolUtils.readCollection(buf, PossibleEffect::read) : null
      );
    }

    public record PossibleEffect(MobEffect effect, float probability) {

      public void write(ByteBuf buf) {
        this.effect.write(buf);
        buf.writeFloat(this.probability);
      }

      public static PossibleEffect read(ByteBuf buf) {
        return new PossibleEffect(MobEffect.read(buf), buf.readFloat());
      }
    }
  }
}
