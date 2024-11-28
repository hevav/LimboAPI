package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class VarIntArrayComponent extends AbstractItemComponent<int[]> {

  private final int limit;

  public VarIntArrayComponent(int limit) {
    this.limit = limit;
  }

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readVarIntArray(buf, this.limit));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeVarIntArray(buf, this.getValue(), this.limit);
  }
}
