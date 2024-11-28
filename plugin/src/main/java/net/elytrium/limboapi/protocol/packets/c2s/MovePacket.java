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

package net.elytrium.limboapi.protocol.packets.c2s;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

public class MovePacket implements MinecraftPacket {

  private double posX;
  private double posY;
  private double posZ;
  private float yaw;
  private float pitch;
  private boolean onGround;
  private boolean collideHorizontally;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.posX = buf.readDouble();
    this.posY = buf.readDouble();
    if (protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.skipBytes(Double.BYTES); // eyes pos
    }
    this.posZ = buf.readDouble();
    this.yaw = buf.readFloat();
    this.pitch = buf.readFloat();
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      this.onGround = buf.readBoolean();
    } else {
      int flags = buf.readUnsignedByte();
      this.onGround = (flags & 1) != 0;
      this.collideHorizontally = (flags & 2) != 0;
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return !(handler instanceof LimboSessionHandlerImpl limbo) || limbo.handle(this);
  }

  @Override
  public int expectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    return version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6) ? Double.BYTES * 4 + Float.BYTES * 2 + 1 : Double.BYTES * 3 + Float.BYTES * 2 + 1;
  }

  @Override
  public int expectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    return Double.BYTES * 3 + Float.BYTES * 2 + 1;
  }

  public double getX() {
    return this.posX;
  }

  public double getY() {
    return this.posY;
  }

  public double getZ() {
    return this.posZ;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public boolean isOnGround() {
    return this.onGround;
  }

  public boolean isCollideHorizontally() {
    return this.collideHorizontally;
  }

  @Override
  public String toString() {
    return "MovePacket{"
        + "posX=" + this.posX
        + ", posY=" + this.posY
        + ", posZ=" + this.posZ
        + ", yaw=" + this.yaw
        + ", pitch=" + this.pitch
        + ", onGround=" + this.onGround
        + ", collideHorizontally=" + this.collideHorizontally
        + "}";
  }
}
