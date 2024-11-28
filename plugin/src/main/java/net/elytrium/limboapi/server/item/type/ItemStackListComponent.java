package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.api.protocol.packets.data.ItemStack;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class ItemStackListComponent extends AbstractItemComponent<Collection<ItemStack>> {

  private final boolean allowEmpty;
  private final int limit;

  public ItemStackListComponent(boolean allowEmpty) {
    this(allowEmpty, Integer.MAX_VALUE);
  }

  public ItemStackListComponent(boolean allowEmpty, int limit) {
    this.allowEmpty = allowEmpty;
    this.limit = limit;
  }

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(LimboProtocolUtils.readCollection(buf, this.limit, () -> LimboProtocolUtils.readItemStack(buf, version, this.allowEmpty)));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    LimboProtocolUtils.writeCollection(buf, this.getValue(), this.limit, itemStack -> LimboProtocolUtils.writeItemStack(buf, version, itemStack, this.allowEmpty));
  }
}
