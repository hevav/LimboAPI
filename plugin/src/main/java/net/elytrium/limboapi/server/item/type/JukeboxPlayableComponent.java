package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.SoundEvent;
import net.elytrium.limboapi.server.item.type.data.VersionLessComponentHolder;

public class JukeboxPlayableComponent extends AbstractItemComponent<JukeboxPlayableComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(JukeboxPlayableComponent.Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(JukeboxSong song, boolean showInTooltip) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      this.song.write(buf, version);
      buf.writeBoolean(this.showInTooltip);
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(JukeboxSong.read(buf, version), buf.readBoolean());
    }

    public sealed interface JukeboxSong permits JukeboxSong.DirectJukeboxSong, JukeboxSong.ReferenceJukeboxSong {

      void write(ByteBuf buf, ProtocolVersion version);

      static JukeboxSong read(ByteBuf buf, ProtocolVersion version) {
        return LimboProtocolUtils.readEither(buf, () -> DirectJukeboxSong.read(buf, version), () -> ReferenceJukeboxSong.read(buf));
      }

      record DirectJukeboxSong(SoundEvent soundEvent, VersionLessComponentHolder description, float lengthInSeconds, int comparatorOutput) implements JukeboxSong {

        @Override
        public void write(ByteBuf buf, ProtocolVersion version) {
          this.soundEvent.write(buf);
          this.description.write(buf, version);
          buf.writeFloat(this.lengthInSeconds);
          ProtocolUtils.writeVarInt(buf, this.comparatorOutput);
        }

        public static DirectJukeboxSong read(ByteBuf buf, ProtocolVersion version) {
          return new DirectJukeboxSong(SoundEvent.read(buf), VersionLessComponentHolder.read(buf, version), buf.readFloat(), ProtocolUtils.readVarInt(buf));
        }
      }

      record ReferenceJukeboxSong(String id) implements JukeboxSong {

        @Override
        public void write(ByteBuf buf, ProtocolVersion version) {
          ProtocolUtils.writeString(buf, this.id);
        }

        public static ReferenceJukeboxSong read(ByteBuf buf) {
          return new ReferenceJukeboxSong(ProtocolUtils.readString(buf));
        }
      }
    }
  }
}
