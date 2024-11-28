package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class BannerPatternLayersComponent extends AbstractItemComponent<Collection<BannerPatternLayersComponent.Layer>> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readCollection(buf, Layer::read));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeCollection(buf, this.getValue(), Layer::write);
  }

  public record Layer(BannerPattern pattern, int color) {

    public void write(ByteBuf buf) {
      this.pattern.write(buf);
      ProtocolUtils.writeVarInt(buf, this.color);
    }

    public static Layer read(ByteBuf buf) {
      return new Layer(BannerPattern.read(buf), ProtocolUtils.readVarInt(buf));
    }

    public sealed interface BannerPattern permits BannerPattern.ReferenceBannerPattern, BannerPattern.DirectBannerPattern {

      void write(ByteBuf buf);

      static BannerPattern read(ByteBuf buf) {
        int i = ProtocolUtils.readVarInt(buf);
        return i == 0 ? DirectBannerPattern.read(buf) : new ReferenceBannerPattern(i - 1);
      }

      record ReferenceBannerPattern(int id) implements BannerPattern {

        @Override
        public void write(ByteBuf buf) {
          ProtocolUtils.writeVarInt(buf, this.id + 1);
        }
      }

      record DirectBannerPattern(String assetId, String translationKey) implements BannerPattern {

        @Override
        public void write(ByteBuf buf) {
          ProtocolUtils.writeString(buf, this.assetId);
          ProtocolUtils.writeString(buf, this.translationKey);
        }

        public static DirectBannerPattern read(ByteBuf buf) {
          return new DirectBannerPattern(ProtocolUtils.readString(buf), ProtocolUtils.readString(buf));
        }
      }
    }
  }
}
