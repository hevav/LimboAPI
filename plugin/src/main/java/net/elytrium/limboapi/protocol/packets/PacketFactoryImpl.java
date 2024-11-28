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

package net.elytrium.limboapi.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.BlockEntityVersion;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.PacketFactory;
import net.elytrium.limboapi.api.protocol.packets.data.EntityDataValue;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.elytrium.limboapi.protocol.packets.s2c.BlockEntityDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.ChunkDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.ChunkUnloadPacket;
import net.elytrium.limboapi.protocol.packets.s2c.DefaultSpawnPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.GameEventPacket;
import net.elytrium.limboapi.protocol.packets.s2c.LightUpdatePacket;
import net.elytrium.limboapi.protocol.packets.s2c.MapDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerAbilitiesPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetChunkCacheCenterPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetEntityDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetExperiencePacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetSlotPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetTimePacket;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateSignPacket;
import net.elytrium.limboapi.protocol.packets.s2c.UpdateTagsPacket;
import net.elytrium.limboapi.server.world.SimpleTagManager;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PacketFactoryImpl implements PacketFactory {

  @Override
  public Object createGameEventPacket(int event, float param) {
    return new GameEventPacket(event, param);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void prepareCompleteChunkDataPacket(ProtocolVersion minPrepareVersion, ProtocolVersion maxPrepareVersion, PreparedPacket packet, ChunkSnapshot chunkSnapshot, boolean hasSkyLight, int maxSections) {
    var flowerPots = minPrepareVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2) ? ChunkDataPacket.getAdditionalFlowerPots(chunkSnapshot) : null;
    packet.prepare(new ChunkDataPacket(chunkSnapshot, hasSkyLight, maxSections, flowerPots == null ? List.of() : flowerPots));
    packet.prepare(
        chunkSnapshot.blockEntityEntriesStream(ProtocolVersion.MINIMUM_VERSION)
            .filter(entry -> entry.getBlockEntity().isSupportedOn(BlockEntityVersion.LEGACY))
            .map(entry -> entry.getBlockEntity().getId(BlockEntityVersion.LEGACY) == 9 ? this.createUpdateSignPacket(entry) : this.createBlockEntityDataPacket(entry))
            .toArray(),
        ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_9_2
    );
    if (flowerPots != null) {
      for (VirtualBlockEntity.Entry entry : flowerPots) {
        packet.prepare(this.createBlockEntityDataPacket(entry), ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_9_2);
      }
    }
    if (maxPrepareVersion.noLessThan(ProtocolVersion.MINECRAFT_1_14) && minPrepareVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_17_1)) {
      packet.prepare(this.createLightUpdatePacket(chunkSnapshot, hasSkyLight), ProtocolVersion.MINECRAFT_1_14, ProtocolVersion.MINECRAFT_1_17_1);
    }
  }

  @Override
  public Object createChunkDataPacket(ChunkSnapshot chunkSnapshot, boolean hasSkyLight, int maxSections) {
    return new ChunkDataPacket(chunkSnapshot, hasSkyLight, maxSections, null);
  }

  @Override
  public Object createLightUpdatePacket(ChunkSnapshot chunkSnapshot, boolean hasSkyLight) {
    return new LightUpdatePacket(chunkSnapshot, hasSkyLight);
  }

  @Override
  public Object createBlockEntityDataPacket(VirtualBlockEntity.Entry entry) {
    return new BlockEntityDataPacket(entry);
  }

  @Override
  public Object createUpdateSignPacket(int posX, int posY, int posZ, @NonNull Component @NonNull [] lines) {
    return new UpdateSignPacket(posX, posY, posZ, lines);
  }

  @Override
  public Object createUpdateSignPacket(VirtualBlockEntity.Entry entry) {
    return new UpdateSignPacket(entry);
  }

  @Override
  public Object createChunkUnloadPacket(int posX, int posZ) {
    return new ChunkUnloadPacket(posX, posZ);
  }

  @Override
  public Object createDefaultSpawnPositionPacket(int posX, int posY, int posZ, float angle) {
    return new DefaultSpawnPositionPacket(posX, posY, posZ, angle);
  }

  @Override
  public Object createMapDataPacket(int mapId, byte scale, MapData mapData) {
    return new MapDataPacket(mapId, scale, mapData);
  }

  @Override
  public Object createEntityDataPacket(int id, Collection<EntityDataValue<?>> packedItems) {
    return new SetEntityDataPacket(id, packedItems);
  }

  @Override
  public Object createPlayerAbilitiesPacket(int abilities, float flyingSpeed, float walkingSpeed) {
    return new PlayerAbilitiesPacket((byte) abilities, flyingSpeed, walkingSpeed);
  }

  @Override
  public Object createPlayerAbilitiesPacket(byte abilities, float flyingSpeed, float walkingSpeed) {
    return new PlayerAbilitiesPacket(abilities, flyingSpeed, walkingSpeed);
  }

  @Override
  public Object createPlayerPositionPacket(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, int teleportId, boolean dismountVehicle) {
    return new PlayerPositionPacket(posX, posY, posZ, yaw, pitch, onGround, teleportId, dismountVehicle);
  }

  @Override
  public Object createSetExperiencePacket(float experienceProgress, int experienceLevel, int totalExperience) {
    return new SetExperiencePacket(experienceProgress, experienceLevel, totalExperience);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count) {
    return new SetSlotPacket(containerId, slot, item, count);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, @Nullable CompoundBinaryTag nbt) {
    return new SetSlotPacket(containerId, slot, item, count, nbt);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, @Nullable ItemComponentMap map) {
    return new SetSlotPacket(containerId, slot, item, count, map);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data) {
    return new SetSlotPacket(containerId, slot, item, count, data);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt) {
    return new SetSlotPacket(containerId, slot, item, count, data, nbt);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable ItemComponentMap map) {
    return new SetSlotPacket(containerId, slot, item, count, data, map);
  }

  @Override
  public Object createSetSlotPacket(int containerId, int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt, @Nullable ItemComponentMap map) {
    return new SetSlotPacket(containerId, slot, item, count, data, nbt, map);
  }

  @Override
  public Object createSetTimePacket(long gameTime, long dayTime) {
    return new SetTimePacket(gameTime, dayTime);
  }

  @Override
  public Object createSetChunkCacheCenter(int posX, int posZ) {
    return new SetChunkCacheCenterPacket(posX, posZ);
  }

  @Override
  public Object createUpdateTagsPacket(WorldVersion version) {
    return SimpleTagManager.getUpdateTagsPacket(version);
  }

  @Override
  public Object createUpdateTagsPacket(ProtocolVersion version) {
    return SimpleTagManager.getUpdateTagsPacket(version);
  }

  @Override
  public Object createUpdateTagsPacket(Map<String, Map<String, int[]>> tags) {
    return new UpdateTagsPacket(tags);
  }
}
