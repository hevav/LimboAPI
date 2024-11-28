/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.data.AbilityFlags;
import net.elytrium.limboapi.api.protocol.packets.data.EntityDataValue;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface LimboPlayer {

  void writePacket(Object msg);

  void writePacketAndFlush(Object msg);

  void flushPackets();

  void closeWith(Object msg);

  ScheduledExecutorService getScheduledExecutor();

  default void sendImage(BufferedImage image) {
    this.sendImage(0, image, true, 36, true);
  }

  default void sendImage(BufferedImage image, boolean sendItem) {
    this.sendImage(0, image, sendItem, 36, true);
  }

  default void sendImage(int mapId, BufferedImage image) {
    this.sendImage(mapId, image, true, 36, true);
  }

  default void sendImage(int mapId, BufferedImage image, boolean sendItem) {
    this.sendImage(mapId, image, sendItem, 36, true);
  }

  default void sendImage(int mapId, BufferedImage image, boolean sendItem, boolean resize) {
    this.sendImage(mapId, image, sendItem, 36, resize);
  }

  void sendImage(int mapId, BufferedImage image, boolean sendItem, int itemSlot, boolean resize);

  @Deprecated(forRemoval = true)
  default void setInventory(VirtualItem item, int count) {
    this.setItemInMainHand(item, count);
  }

  @Deprecated(forRemoval = true)
  default void setInventory(VirtualItem item, int slot, int count) {
    this.setItem(slot, item, count);
  }

  @Deprecated(forRemoval = true)
  default void setInventory(int slot, VirtualItem item, int count, int data, CompoundBinaryTag nbt) {
    this.setItem(slot, item, count, (short) data, nbt, null);
  }

  @Deprecated(forRemoval = true)
  default void setInventory(int slot, VirtualItem item, int count, int data, ItemComponentMap map) {
    this.setItem(slot, item, count, (short) data, null, map);
  }

  void setItemInMainHand(VirtualItem item, int count);

  void setItemInOffHand(VirtualItem item, int count);

  void setItem(int slot, VirtualItem item, int count);

  void setItem(int slot, VirtualItem item, int count, @Nullable CompoundBinaryTag nbt);

  void setItem(int slot, VirtualItem item, int count, @Nullable ItemComponentMap map);

  void setItem(int slot, VirtualItem item, int count, short data);

  void setItem(int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt);

  void setItem(int slot, VirtualItem item, int count, short data, @Nullable ItemComponentMap map);

  void setItem(int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt, @Nullable ItemComponentMap map);

  void setGameMode(GameMode gameMode);

  void setWorldTime(long ticks);

  void teleport(double posX, double posY, double posZ, float yaw, float pitch);

  void disableFalling();

  void enableFalling();

  void disconnect();

  void disconnect(RegisteredServer server);

  /**
   * @param id Entity id
   */
  void setEntityData(int id, Collection<EntityDataValue<?>> packedItems);

  @Deprecated(forRemoval = true)
  default void sendAbilities() {
    this.sendGameModeSpecificAbilities();
  }

  /**
   * @apiNote Also resets flyingSpeed and walkingSpeed
   */
  void sendGameModeSpecificAbilities();

  /**
   * @param abilities See {@link AbilityFlags} (e.g. {@code AbilityFlags.ALLOW_FLYING | AbilityFlags.CREATIVE_MODE})
   */
  void sendAbilities(int abilities, float flyingSpeed, float walkingSpeed);

  /**
   * @param abilities See {@link AbilityFlags} (e.g. {@code AbilityFlags.ALLOW_FLYING | AbilityFlags.CREATIVE_MODE})
   */
  void sendAbilities(byte abilities, float flyingSpeed, float walkingSpeed);

  @Deprecated(forRemoval = true)
  default byte getAbilities() {
    return this.getGameModeSpecificAbilities();
  }

  byte getGameModeSpecificAbilities();

  GameMode getGameMode();

  Limbo getServer();

  Player getProxyPlayer();

  int getPing();
}
