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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.utils.Reflection;

@SuppressWarnings("unchecked")
public class UpsertPlayerInfoHook extends UpsertPlayerInfoPacket {

  static final MethodHandle SERVER_CONN_FIELD = Reflection.findGetter(BackendPlaySessionHandler.class, "serverConn", VelocityServerConnection.class);

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    if (handler instanceof BackendPlaySessionHandler) {
      try {
        ConnectedPlayer player = ((VelocityServerConnection) SERVER_CONN_FIELD.invokeExact((BackendPlaySessionHandler) handler)).getPlayer();
        UUID initialId = LimboAPI.getClientUniqueId(player);
        List<Entry> items = this.getEntries();
        for (int i = 0; i < items.size(); ++i) {
          Entry item = items.get(i);
          if (player.getUniqueId().equals(item.getProfileId())) {
            items.set(i, UpsertPlayerInfoHook.createFixedEntry(initialId, item, player));
          }
        }
      } catch (Throwable t) {
        throw new ReflectionException(t);
      }
    }

    return super.handle(handler);
  }

  private static Entry createFixedEntry(UUID initialId, Entry item, ConnectedPlayer player) {
    Entry fixedEntry = new Entry(initialId);
    GameProfile profile = item.getProfile();
    fixedEntry.setProfile(profile == null || !profile.getId().equals(player.getUniqueId()) ? profile : new GameProfile(initialId, profile.getName(), profile.getProperties()));
    fixedEntry.setListed(item.isListed());
    fixedEntry.setLatency(item.getLatency());
    fixedEntry.setGameMode(item.getGameMode());
    fixedEntry.setDisplayName(item.getDisplayName());
    fixedEntry.setListOrder(item.getListOrder());
    fixedEntry.setChatSession(item.getChatSession());
    return fixedEntry;
  }

  public static void init(StateRegistry.PacketRegistry registry) throws ReflectiveOperationException {
    var playProtocolRegistryVersions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) LimboProtocol.VERSIONS_FIELD.get(registry);
    playProtocolRegistryVersions.forEach((protocolVersion, protocolRegistry) -> {
      try {
        var packetIDToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) LimboProtocol.PACKET_ID_TO_SUPPLIER_FIELD.get(protocolRegistry);
        var packetClassToID = (Object2IntMap<Class<? extends MinecraftPacket>>) LimboProtocol.PACKET_CLASS_TO_ID_FIELD.get(protocolRegistry);

        int id = packetClassToID.getInt(UpsertPlayerInfoPacket.class);
        packetClassToID.put(UpsertPlayerInfoHook.class, id);
        packetIDToSupplier.put(id, UpsertPlayerInfoHook::new);
      } catch (ReflectiveOperationException e) {
        throw new ReflectionException(e);
      }
    });
  }
}
