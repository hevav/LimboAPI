package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;

public record LightUpdatePacket(int posX, int posZ, LightSection[] light, int[] masks) implements MinecraftPacket {

  public LightUpdatePacket(ChunkSnapshot chunk, boolean hasSkyLight) {
    this(chunk.posX(), chunk.posZ(), chunk.light(), LightUpdatePacket.calcMasks(chunk, hasSkyLight));
  }

  // TODO get rid of this method when JEP 447
  private static int[] calcMasks(ChunkSnapshot chunk, boolean hasSkyLight) {
    boolean fullChunk = chunk.fullChunk();
    int skyLightMask = fullChunk ? 0x3FFFF : 0;
    int blockLightMask = 0;
    var sections = chunk.sections();
    for (int i = 0; i < sections.length; ++i) {
      if (sections[i] != null) {
        if (!fullChunk && hasSkyLight) {
          skyLightMask |= (1 << i);
        }

        blockLightMask |= (1 << i);
      }
    }

    return new int[] {
        skyLightMask,
        blockLightMask
    };
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.posX);
    ProtocolUtils.writeVarInt(buf, this.posZ);
    ChunkDataPacket.writeLight(buf, protocolVersion, this.light, this.masks[0], this.masks[1]);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
