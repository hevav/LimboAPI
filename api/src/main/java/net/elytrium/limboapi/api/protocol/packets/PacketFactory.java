/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Collection;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.data.AbilityFlags;
import net.elytrium.limboapi.api.protocol.packets.data.EntityDataValue;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PacketFactory {

  @Deprecated(forRemoval = true)
  default Object createChangeGameStatePacket(int event, float param) {
    return this.createGameEventPacket(event, param);
  }

  Object createGameEventPacket(int event, float param);

  /**
   * @see #prepareCompleteChunkDataPacket(ProtocolVersion, ProtocolVersion, PreparedPacket, ChunkSnapshot, Dimension)
   */
  default void prepareCompleteChunkDataPacket(PreparedPacket packet, ChunkSnapshot chunkSnapshot, Dimension dimension) {
    this.prepareCompleteChunkDataPacket(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION, packet, chunkSnapshot, dimension);
  }

  /**
   * @see #prepareCompleteChunkDataPacket(ProtocolVersion, ProtocolVersion, PreparedPacket, ChunkSnapshot, Dimension)
   */
  default void prepareCompleteChunkDataPacket(ProtocolVersion minPrepareVersion, ProtocolVersion maxPrepareVersion, PreparedPacket packet, ChunkSnapshot chunkSnapshot, Dimension dimension) {
    this.prepareCompleteChunkDataPacket(minPrepareVersion, maxPrepareVersion, packet, chunkSnapshot, dimension.hasSkyLight(), dimension.getMaxSections());
  }

  /**
   * @see #prepareCompleteChunkDataPacket(ProtocolVersion, ProtocolVersion, PreparedPacket, ChunkSnapshot, Dimension)
   */
  default void prepareCompleteChunkDataPacket(PreparedPacket packet, ChunkSnapshot chunkSnapshot, boolean hasSkyLight, int maxSections) {
    this.prepareCompleteChunkDataPacket(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION, packet, chunkSnapshot, hasSkyLight, maxSections);
  }

  /**
   * Prepares a complete chunk that includes various packets depending on the protocol version:
   * <ul>
   *   <li>{@link #createLightUpdatePacket(ChunkSnapshot, boolean) Light Update Packet} - for versions [1.14-1.17.1]</li>
   *   <li>{@link #createBlockEntityDataPacket(VirtualBlockEntity.Entry) Block Entity Data Packet} - for versions [1.7.2-1.9.2]</li>
   *   <li>{@link #createUpdateSignPacket(VirtualBlockEntity.Entry) Update Sign Packet} - for versions [1.7.2-1.9.2], if applicable</li>
   * </ul>
   */
  void prepareCompleteChunkDataPacket(ProtocolVersion minPrepareVersion, ProtocolVersion maxPrepareVersion, PreparedPacket packet, ChunkSnapshot chunkSnapshot, boolean hasSkyLight, int maxSections);

  default Object createChunkDataPacket(ChunkSnapshot chunkSnapshot, Dimension dimension) {
    return this.createChunkDataPacket(chunkSnapshot, dimension.hasSkyLight(), dimension.getMaxSections());
  }

  Object createChunkDataPacket(ChunkSnapshot chunkSnapshot, boolean hasSkyLight, int maxSections);

  /**
   * @sinceMinecraft 1.14
   */
  default Object createLightUpdatePacket(ChunkSnapshot chunkSnapshot, Dimension dimension) {
    return this.createLightUpdatePacket(chunkSnapshot, dimension.hasSkyLight());
  }

  /**
   * @sinceMinecraft 1.14
   */
  Object createLightUpdatePacket(ChunkSnapshot chunkSnapshot, boolean hasSkyLight);

  Object createBlockEntityDataPacket(VirtualBlockEntity.Entry entry);

  /**
   * @param lines length should be exactly 4
   *
   * @deprecated Used only in [1.7.2-1.9.2]
   */
  @Deprecated
  Object createUpdateSignPacket(int posX, int posY, int posZ, @NonNull Component @NonNull [] lines);

  /**
   * @deprecated Used only in [1.7.2-1.9.2]
   */
  @Deprecated
  Object createUpdateSignPacket(VirtualBlockEntity.Entry entry);

  Object createChunkUnloadPacket(int posX, int posZ);

  Object createDefaultSpawnPositionPacket(int posX, int posY, int posZ, float angle);

  Object createMapDataPacket(int mapId, byte scale, MapData mapData);

  /**
   * @param id Entity id
   */
  Object createEntityDataPacket(int id, Collection<EntityDataValue<?>> packedItems);

  /**
   * @param abilities See {@link AbilityFlags} (e.g. {@code AbilityFlags.ALLOW_FLYING | AbilityFlags.CREATIVE_MODE})
   */
  Object createPlayerAbilitiesPacket(int abilities, float flyingSpeed, float walkingSpeed);

  /**
   * @param abilities See {@link AbilityFlags} (e.g. {@code AbilityFlags.ALLOW_FLYING | AbilityFlags.CREATIVE_MODE})
   */
  Object createPlayerAbilitiesPacket(byte abilities, float flyingSpeed, float walkingSpeed);

  @Deprecated(forRemoval = true)
  default Object createPositionRotationPacket(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, int teleportId, boolean dismountVehicle) {
    return this.createPlayerPositionPacket(posX, posY, posZ, yaw, pitch, onGround, teleportId, dismountVehicle);
  }

  Object createPlayerPositionPacket(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, int teleportId, boolean dismountVehicle);

  Object createSetExperiencePacket(float experienceProgress, int experienceLevel, int totalExperience);

  @Deprecated(forRemoval = true)
  default Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, int data, @Nullable CompoundBinaryTag nbt) {
    return this.createSetSlotPacket(containerId, slot, item, count, (short) data, nbt);
  }

  @Deprecated(forRemoval = true)
  default Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, int data, @Nullable ItemComponentMap map) {
    return this.createSetSlotPacket(containerId, slot, item, count, (short) data, map);
  }

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count);

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, @Nullable CompoundBinaryTag nbt);

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, @Nullable ItemComponentMap map);

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data);

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt);

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable ItemComponentMap map);

  Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt, @Nullable ItemComponentMap map);

  @Deprecated(forRemoval = true)
  default Object createTimeUpdatePacket(long gameTime, long dayTime) {
    return this.createSetTimePacket(gameTime, dayTime);
  }

  Object createSetTimePacket(long gameTime, long dayTime);

  @Deprecated(forRemoval = true)
  default Object createUpdateViewPositionPacket(int posX, int posZ) {
    return this.createSetChunkCacheCenter(posX, posZ);
  }

  /**
   * @sinceMinecraft 1.14
   */
  Object createSetChunkCacheCenter(int posX, int posZ);

  Object createUpdateTagsPacket(WorldVersion version);

  Object createUpdateTagsPacket(ProtocolVersion version);

  /**
   * @sinceMinecraft 1.13
   */
  Object createUpdateTagsPacket(Map<String, Map<String, int[]>> tags);
}
