package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.HolderSet;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AdventureModePredicateComponent extends AbstractItemComponent<AdventureModePredicateComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(Collection<BlockPredicate> predicates, boolean showInTooltip) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      LimboProtocolUtils.writeCollection(buf, this.predicates, predicate -> predicate.write(buf, version));
      buf.writeBoolean(this.showInTooltip);
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(LimboProtocolUtils.readCollection(buf, () -> BlockPredicate.read(buf, version)), buf.readBoolean());
    }

    public record BlockPredicate(@Nullable HolderSet blocks, @Nullable Collection<PropertyMatcher> properties, @Nullable CompoundBinaryTag nbt) {

      public void write(ByteBuf buf, ProtocolVersion version) {
        LimboProtocolUtils.writeOptional(buf, this.blocks, HolderSet::write);
        LimboProtocolUtils.writeOptional(buf, this.properties, properties -> LimboProtocolUtils.writeCollection(buf, properties, PropertyMatcher::write));
        LimboProtocolUtils.writeOptional(buf, this.nbt, nbt -> ProtocolUtils.writeBinaryTag(buf, version, nbt));
      }

      public static BlockPredicate read(ByteBuf buf, ProtocolVersion version) {
        return new BlockPredicate(
            LimboProtocolUtils.readOptional(buf, HolderSet::read),
            LimboProtocolUtils.readOptional(buf, () -> LimboProtocolUtils.readCollection(buf, PropertyMatcher::read)),
            LimboProtocolUtils.readOptional(buf, () -> ProtocolUtils.readCompoundTag(buf, version, null))
        );
      }

      public record PropertyMatcher(String name, ValueMatcher valueMatcher) {

        public void write(ByteBuf buf) {
          ProtocolUtils.writeString(buf, this.name);
          this.valueMatcher.write(buf);
        }

        public static PropertyMatcher read(ByteBuf buf) {
          return new PropertyMatcher(ProtocolUtils.readString(buf), ValueMatcher.read(buf));
        }

        public sealed interface ValueMatcher permits ValueMatcher.ExactMatcher, ValueMatcher.RangedMatcher {

          void write(ByteBuf buf);

          static ValueMatcher read(ByteBuf buf) {
            return LimboProtocolUtils.readEither(buf, ExactMatcher::read, RangedMatcher::read);
          }

          record ExactMatcher(String value) implements ValueMatcher {

            @Override
            public void write(ByteBuf buf) {
              ProtocolUtils.writeString(buf, this.value);
            }

            public static ExactMatcher read(ByteBuf buf) {
              return new ExactMatcher(ProtocolUtils.readString(buf));
            }
          }

          record RangedMatcher(@Nullable String minValue, @Nullable String maxValue) implements ValueMatcher {

            @Override
            public void write(ByteBuf buf) {
              LimboProtocolUtils.writeOptional(buf, this.minValue, ProtocolUtils::writeString);
              LimboProtocolUtils.writeOptional(buf, this.maxValue, ProtocolUtils::writeString);
            }

            public static RangedMatcher read(ByteBuf buf) {
              return new RangedMatcher(LimboProtocolUtils.readOptional(buf, ProtocolUtils::readString), LimboProtocolUtils.readOptional(buf, ProtocolUtils::readString));
            }
          }
        }
      }
    }
  }
}
