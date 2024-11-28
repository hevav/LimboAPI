package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.VersionLessComponentHolder;

public class ArmorTrimComponent extends AbstractItemComponent<ArmorTrimComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(TrimMaterial material, TrimPattern pattern, boolean showInTooltip) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      this.material.write(buf, version);
      this.pattern.write(buf, version);
      buf.writeBoolean(this.showInTooltip);
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(TrimMaterial.read(buf, version), TrimPattern.read(buf, version), buf.readBoolean());
    }

    public sealed interface TrimMaterial permits TrimMaterial.ReferenceTrimMaterial, TrimMaterial.DirectTrimMaterial {

      void write(ByteBuf buf, ProtocolVersion version);

      static TrimMaterial read(ByteBuf buf, ProtocolVersion version) {
        int i = ProtocolUtils.readVarInt(buf);
        return i == 0 ? DirectTrimMaterial.read(buf, version) : new ReferenceTrimMaterial(i - 1);
      }

      record ReferenceTrimMaterial(int id) implements TrimMaterial {

        @Override
        public void write(ByteBuf buf, ProtocolVersion version) {
          ProtocolUtils.writeVarInt(buf, this.id + 1);
        }
      }

      record DirectTrimMaterial(String assetName, int ingredient, float itemModelIndex, Map<String, String> overrideArmorMaterials, VersionLessComponentHolder description) implements TrimMaterial {

        @Override
        public void write(ByteBuf buf, ProtocolVersion version) {
          ProtocolUtils.writeString(buf, this.assetName);
          ProtocolUtils.writeVarInt(buf, this.ingredient);
          buf.writeFloat(this.itemModelIndex);
          LimboProtocolUtils.writeMap(buf, this.overrideArmorMaterials, ProtocolUtils::writeString, ProtocolUtils::writeString);
          this.description.write(buf, version);
        }

        public static DirectTrimMaterial read(ByteBuf buf, ProtocolVersion version) {
          return new DirectTrimMaterial(
              ProtocolUtils.readString(buf),
              ProtocolUtils.readVarInt(buf),
              buf.readFloat(),
              LimboProtocolUtils.readMap(buf, ProtocolUtils::readString, ProtocolUtils::readString),
              VersionLessComponentHolder.read(buf, version)
          );
        }
      }
    }

    public sealed interface TrimPattern permits TrimPattern.ReferenceTrimPattern, TrimPattern.DirectTrimPattern {

      void write(ByteBuf buf, ProtocolVersion version);

      static TrimPattern read(ByteBuf buf, ProtocolVersion version) {
        int i = ProtocolUtils.readVarInt(buf);
        return i == 0 ? DirectTrimPattern.read(buf, version) : new ReferenceTrimPattern(i - 1);
      }

      record ReferenceTrimPattern(int id) implements TrimPattern {

        @Override
        public void write(ByteBuf buf, ProtocolVersion version) {
          ProtocolUtils.writeVarInt(buf, this.id + 1);
        }
      }

      record DirectTrimPattern(String assetId, int templateItem, VersionLessComponentHolder description, boolean decal) implements TrimPattern {

        @Override
        public void write(ByteBuf buf, ProtocolVersion version) {
          ProtocolUtils.writeString(buf, this.assetId);
          ProtocolUtils.writeVarInt(buf, this.templateItem);
          this.description.write(buf, version);
          buf.writeBoolean(this.decal);
        }

        public static DirectTrimPattern read(ByteBuf buf, ProtocolVersion version) {
          return new DirectTrimPattern(ProtocolUtils.readString(buf), ProtocolUtils.readVarInt(buf), VersionLessComponentHolder.read(buf, version), buf.readBoolean());
        }
      }
    }
  }
}
