package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.HolderSet;

public class HolderSetComponent extends AbstractItemComponent<HolderSet> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(HolderSet.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }
}
