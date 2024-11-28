package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.FilterableString;

public class WritableBookContentComponent extends AbstractItemComponent<Collection<FilterableString>> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readCollection(buf, 100, FilterableString::read));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeCollection(buf, this.getValue(), 100, FilterableString::write);
  }
}
