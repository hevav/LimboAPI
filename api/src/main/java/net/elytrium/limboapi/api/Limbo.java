/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.Player;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;

public interface Limbo {

  void spawnPlayer(Player player, LimboSessionHandler handler);

  void respawnPlayer(Player player);

  long getCurrentOnline();

  Limbo setName(String name);

  Limbo setReadTimeout(int millis);

  Limbo setWorldTime(long ticks);

  Limbo setGameMode(GameMode gameMode);

  Limbo setShouldRejoin(boolean shouldRejoin);

  Limbo setShouldRespawn(boolean shouldRespawn);

  Limbo setShouldUpdateTags(boolean shouldUpdateTags);

  Limbo setReducedDebugInfo(boolean reducedDebugInfo);

  Limbo setViewDistance(int viewDistance);

  Limbo setSimulationDistance(int simulationDistance);

  Limbo setMaxSuppressPacketLength(int maxSuppressPacketLength);

  /**
   * @deprecated Use {@link com.velocitypowered.api.command.CommandManager#metaBuilder(String)}
   */
  @Deprecated(forRemoval = true)
  default Limbo registerCommand(net.elytrium.limboapi.api.command.LimboCommandMeta commandMeta) {
    return this.registerCommand((CommandMeta) commandMeta);
  }

  Limbo registerCommand(CommandMeta commandMeta);

  Limbo registerCommand(CommandMeta commandMeta, Command command);

  Limbo registerPacket(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] packetMappings);

  void dispose();
}
