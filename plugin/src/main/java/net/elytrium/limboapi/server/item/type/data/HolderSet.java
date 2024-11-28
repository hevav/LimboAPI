package net.elytrium.limboapi.server.item.type.data;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public sealed interface HolderSet permits HolderSet.NamedHolderSet, HolderSet.DirectHolderSet {

  void write(ByteBuf buf);

  static HolderSet read(ByteBuf buf) {
    int i = ProtocolUtils.readVarInt(buf);
    return i == 0 ? NamedHolderSet.read(buf) : DirectHolderSet.read(buf, i - 1);
  }

  record NamedHolderSet(String key) implements HolderSet {

    @Override
    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, 0);
      ProtocolUtils.writeString(buf, this.key);
    }

    public static NamedHolderSet read(ByteBuf buf) {
      return new NamedHolderSet(ProtocolUtils.readString(buf));
    }
  }

  record DirectHolderSet(int[] contents) implements HolderSet {

    @Override
    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.contents.length + 1);
      for (int block : this.contents) {
        ProtocolUtils.writeVarInt(buf, block);
      }
    }

    public static DirectHolderSet read(ByteBuf buf, int amount) {
      int[] blocks = new int[amount];
      for (int i = 0; i < amount; ++i) {
        blocks[i] = ProtocolUtils.readVarInt(buf);
      }

      return new DirectHolderSet(blocks);
    }
  }
}
