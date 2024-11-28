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
import java.util.Map;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;

public record UpdateTagsPacket(Map<String, Map<String, int[]>> tags) implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      LimboProtocolUtils.writeMap(buf, this.tags, ProtocolUtils::writeString, UpdateTagsPacket::writeTags);
    } else {
      UpdateTagsPacket.writeTags(buf, this.tags.get("minecraft:block"));
      UpdateTagsPacket.writeTags(buf, this.tags.get("minecraft:item"));
      UpdateTagsPacket.writeTags(buf, this.tags.get("minecraft:fluid"));
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
        UpdateTagsPacket.writeTags(buf, this.tags.get("minecraft:entity_type"));
      }
    }
  }

  private static void writeTags(ByteBuf buf, Map<String, int[]> tags) {
    LimboProtocolUtils.writeMap(buf, tags, ProtocolUtils::writeString, LimboProtocolUtils::writeVarIntArray);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }
}
