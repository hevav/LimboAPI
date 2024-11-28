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
import net.elytrium.limboapi.api.protocol.packets.data.MapData;

public record MapDataPacket(int mapId, byte scale, MapData mapData) implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.mapId);
    byte[] data = this.mapData.data();
    if (protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeShort((1 + 1 + 1) + data.length);
      buf.writeByte(0); // type (?)
      buf.writeByte(this.mapData.posX());
      buf.writeByte(this.mapData.posY());
      buf.writeBytes(data);
    } else {
      buf.writeByte(this.scale);

      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_9) && protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_16_4)) {
        buf.writeBoolean(false); // trackingPosition
      }

      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
        buf.writeBoolean(false); // locked
      }

      // decorations (their lack, that is)
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        buf.writeBoolean(false);
      } else {
        ProtocolUtils.writeVarInt(buf, 0);
      }

      buf.writeByte(this.mapData.columns());
      buf.writeByte(this.mapData.rows());
      buf.writeByte(this.mapData.posX());
      buf.writeByte(this.mapData.posY());

      ProtocolUtils.writeByteArray(buf, data);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
