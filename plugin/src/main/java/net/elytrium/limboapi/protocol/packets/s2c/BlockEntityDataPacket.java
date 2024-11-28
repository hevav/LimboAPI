package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;

public record BlockEntityDataPacket(VirtualBlockEntity.Entry entry) implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeInt(this.entry.getPosX());
      buf.writeShort(this.entry.getPosY());
      buf.writeInt(this.entry.getPosZ());
    } else {
      LimboProtocolUtils.writeBlockPos(buf, protocolVersion, this.entry.getPosX(), this.entry.getPosY(), this.entry.getPosZ());
    }
    ProtocolUtils.writeVarInt(buf, this.entry.getBlockEntity().getId(protocolVersion));
    LimboProtocolUtils.writeBinaryTag(buf, protocolVersion, this.entry.getNbt(protocolVersion));
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
