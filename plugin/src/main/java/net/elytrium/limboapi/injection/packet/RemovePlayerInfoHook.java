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

package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfoPacket;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.protocol.LimboProtocol;

@SuppressWarnings("unchecked")
public class RemovePlayerInfoHook extends RemovePlayerInfoPacket {

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof BackendPlaySessionHandler) {
      try {
        ConnectedPlayer player = ((VelocityServerConnection) UpsertPlayerInfoHook.SERVER_CONN_FIELD.invokeExact((BackendPlaySessionHandler) handler)).getPlayer();
        UUID initialID = LimboAPI.getClientUniqueId(player);
        if (this.getProfilesToRemove() instanceof List<UUID> uuids) {
          for (int i = 0; i < uuids.size(); i++) {
            if (player.getUniqueId().equals(uuids.get(i))) {
              uuids.set(i, initialID);
            }
          }
        }
      } catch (Throwable t) {
        throw new ReflectionException(t);
      }
    }

    return super.handle(handler);
  }

  public static void init(StateRegistry.PacketRegistry registry) throws ReflectiveOperationException {
    var playProtocolRegistryVersions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) LimboProtocol.VERSIONS_FIELD.get(registry);
    playProtocolRegistryVersions.forEach((protocolVersion, protocolRegistry) -> {
      try {
        var packetIDToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) LimboProtocol.PACKET_ID_TO_SUPPLIER_FIELD.get(protocolRegistry);
        var packetClassToID = (Object2IntMap<Class<? extends MinecraftPacket>>) LimboProtocol.PACKET_CLASS_TO_ID_FIELD.get(protocolRegistry);

        int id = packetClassToID.getInt(RemovePlayerInfoPacket.class);
        packetClassToID.put(RemovePlayerInfoHook.class, id);
        packetIDToSupplier.put(id, RemovePlayerInfoHook::new);
      } catch (ReflectiveOperationException e) {
        throw new ReflectionException(e);
      }
    });
  }
}
