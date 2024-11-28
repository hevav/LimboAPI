package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.UUID;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class AttributeModifiersComponent extends AbstractItemComponent<AttributeModifiersComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(Collection<Entry> modifiers, boolean showInTooltip) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      LimboProtocolUtils.writeCollection(buf, this.modifiers, entry -> entry.write(buf, version));
      buf.writeBoolean(this.showInTooltip);
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(LimboProtocolUtils.readCollection(buf, () -> Entry.read(buf, version)), buf.readBoolean());
    }

    public record Entry(int attribute, AttributeModifier modifier, int slot) {

      public void write(ByteBuf buf, ProtocolVersion version) {
        ProtocolUtils.writeVarInt(buf, this.attribute);
        this.modifier.write(buf, version);
        ProtocolUtils.writeVarInt(buf, this.slot);
      }

      public static Entry read(ByteBuf buf, ProtocolVersion version) {
        return new Entry(ProtocolUtils.readVarInt(buf), AttributeModifier.read(buf, version), ProtocolUtils.readVarInt(buf));
      }

      public record AttributeModifier(UUID uuid, String name, double amount, int operation) {

        public void write(ByteBuf buf, ProtocolVersion version) {
          if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_20_5)) {
            if (this.uuid == null) {
              throw new IllegalArgumentException("uuid cannot be null on 1.20.5!");
            }

            ProtocolUtils.writeUuid(buf, this.uuid);
          }

          ProtocolUtils.writeString(buf, this.name);
          buf.writeDouble(this.amount);
          ProtocolUtils.writeVarInt(buf, this.operation);
        }

        public static AttributeModifier read(ByteBuf buf, ProtocolVersion version) {
          return new AttributeModifier(
              version.noGreaterThan(ProtocolVersion.MINECRAFT_1_20_5) ? ProtocolUtils.readUuid(buf) : null,
              ProtocolUtils.readString(buf),
              buf.readDouble(),
              ProtocolUtils.readVarInt(buf)
          );
        }
      }
    }
  }
}
