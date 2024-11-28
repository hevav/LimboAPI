package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.FilterableComponent;
import net.elytrium.limboapi.server.item.type.data.FilterableString;

public class WrittenBookContentComponent extends AbstractItemComponent<WrittenBookContentComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf, version);
  }

  public record Value(FilterableString title, String author, int generation, Collection<FilterableComponent> pages, boolean resolved) {

    public void write(ByteBuf buf, ProtocolVersion version) {
      this.title.write(buf);
      ProtocolUtils.writeString(buf, this.author);
      ProtocolUtils.writeVarInt(buf, this.generation);
      LimboProtocolUtils.writeCollection(buf, this.pages, page -> page.write(buf, version));
      buf.writeBoolean(this.resolved);
    }

    public static Value read(ByteBuf buf, ProtocolVersion version) {
      return new Value(
          FilterableString.read(buf), ProtocolUtils.readString(buf), ProtocolUtils.readVarInt(buf),
          LimboProtocolUtils.readCollection(buf, () -> FilterableComponent.read(buf, version)),
          buf.readBoolean()
      );
    }
  }
}
