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
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt, @Nullable ItemComponentMap map) implements MinecraftPacket {

  public SetSlotPacket(int containerId, int slot, VirtualItem item, int count) {
    this(containerId, slot, item, count, (short) 0, null, null);
  }

  public SetSlotPacket(int containerId, int slot, VirtualItem item, int count, @Nullable CompoundBinaryTag nbt) {
    this(containerId, slot, item, count, (short) 0, nbt, null);
  }

  public SetSlotPacket(int containerId, int slot, VirtualItem item, int count, @Nullable ItemComponentMap map) {
    this(containerId, slot, item, count, (short) 0, null, map);
  }

  public SetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data) {
    this(containerId, slot, item, count, data, null, null);
  }

  public SetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt) {
    this(containerId, slot, item, count, data, nbt, null);
  }

  public SetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable ItemComponentMap map) {
    this(containerId, slot, item, count, data, null, map);
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    LimboProtocolUtils.writeContainerId(buf, protocolVersion, this.containerId);

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17_1)) {
      ProtocolUtils.writeVarInt(buf, 0); // State id
    }

    buf.writeShort(this.slot);

    int id;
    if (this.count > 0 && (id = this.item.itemId(protocolVersion)) > 0) {
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        ProtocolUtils.writeVarInt(buf, this.count);
        ProtocolUtils.writeVarInt(buf, id);
        if (this.map == null) {
          ProtocolUtils.writeVarInt(buf, 0); // Added
          ProtocolUtils.writeVarInt(buf, 0); // Removed
        } else {
          this.map.write(buf, protocolVersion);
        }
      } else {
        if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
          buf.writeBoolean(true);
          ProtocolUtils.writeVarInt(buf, id);
        } else {
          buf.writeShort(id);
        }

        buf.writeByte(this.count);
        if (protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
          buf.writeShort(this.data);
        }

        LimboProtocolUtils.writeBinaryTag(buf, protocolVersion, this.nbt);
      }
    } else if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
      buf.writeBoolean(false);
    } else {
      buf.writeShort(-1);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
