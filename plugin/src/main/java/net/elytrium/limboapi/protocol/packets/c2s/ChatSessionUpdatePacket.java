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
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.elytrium.limboapi.Settings;

public class ChatSessionUpdatePacket implements MinecraftPacket {

  private UUID sessionId;
  private IdentifiedKey profilePublicKey;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.sessionId = ProtocolUtils.readUuid(buf);
    this.profilePublicKey = ProtocolUtils.readPlayerKey(protocolVersion, buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeUuid(buf, this.sessionId);
    ProtocolUtils.writePlayerKey(buf, this.profilePublicKey);
  }

  @Override
  public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
    // LimboAPI hook - skip server-side signature verification if enabled
    return minecraftSessionHandler instanceof ClientPlaySessionHandler && Settings.IMP.MAIN.FORCE_DISABLE_MODERN_CHAT_SIGNING;
  }

  public UUID getSessionId() {
    return this.sessionId;
  }

  public IdentifiedKey getProfilePublicKey() {
    return this.profilePublicKey;
  }

  @Override
  public String toString() {
    return "PlayerChatSessionPacket{"
           + "holderId=" + this.sessionId
           + ", playerKey=" + this.profilePublicKey
           + "}";
  }
}
