package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.protocol.packets.data.ItemStack;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class ItemStackComponent extends AbstractItemComponent<ItemStack> {

  private final boolean allowEmpty;

  public ItemStackComponent(boolean allowEmpty) {
    this.allowEmpty = allowEmpty;
  }

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readItemStack(buf, version, this.allowEmpty));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeItemStack(buf, version, this.getValue(), this.allowEmpty);
  }
}
