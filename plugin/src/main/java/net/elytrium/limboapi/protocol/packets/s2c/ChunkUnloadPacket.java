/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public record ChunkUnloadPacket(int posX, int posZ) implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    buf.writeLong(protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_20)
        ? (((long) this.posX & 0xFFFFFFFFL) << 32 | (long) this.posZ & 0xFFFFFFFFL)
        : (((long) this.posZ & 0xFFFFFFFFL) << 32 | (long) this.posX & 0xFFFFFFFFL) // >=1.20.2
    );
    if (protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
      // essentially, it's ChunkData, but written in a way that the chunk gets unloaded
      buf.writeBoolean(true);
      buf.writeShort(0);
      if (protocolVersion == ProtocolVersion.MINECRAFT_1_8) {
        ProtocolUtils.writeVarInt(buf, 0);
      } else {
        buf.writeShort(0);
        buf.writeInt(0);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
