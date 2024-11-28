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

public record PlayerPositionPacket(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, int teleportId, boolean dismountVehicle) implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    final boolean v121 = protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_2);
    final boolean v17 = !v121 && protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);

    if (v121) {
      ProtocolUtils.writeVarInt(buf, this.teleportId);
    }

    buf.writeDouble(this.posX);
    buf.writeDouble(v17 ? this.posY + 1.62F/*in 1.7.x posY means eyes position*/ : this.posY);
    buf.writeDouble(this.posZ);
    if (v121) {
      // deltaMovement
      buf.writeDouble(0);
      buf.writeDouble(0);
      buf.writeDouble(0);
    }
    buf.writeFloat(this.yaw);
    buf.writeFloat(this.pitch);

    if (v121) {
      buf.writeInt(0x00); // relatives
    } else if (v17) {
      buf.writeBoolean(this.onGround);
    } else {
      buf.writeByte(0x00); // relativeArguments
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
        ProtocolUtils.writeVarInt(buf, this.teleportId);
      }

      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17) && protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        buf.writeBoolean(this.dismountVehicle);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
