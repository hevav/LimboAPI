/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.BuiltInBiome;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.PacketFactory;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface LimboFactory {

  /**
   * Creates new virtual block from Block enum
   *
   * @param block Block from Block enum
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(Block block);

  /**
   * Creates new virtual block from id and data
   *
   * @param legacyId Legacy block id (1.12.2 and lower)
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(short legacyId);

  /**
   * Creates new virtual block from id and data
   *
   * @param modernId Modern block id
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(String modernId);

  /**
   * Creates new virtual block from id and data
   *
   * @param modernId   Modern block id
   * @param properties Modern properties like {"waterlogged": "true"}
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(String modernId, @Nullable Map<String, String> properties);

  /**
   * Creates new virtual block from id and data
   *
   * @param id     Block id
   * @param modern Use the latest supported version ids or 1.12.2 and lower
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(short id, boolean modern);

  @Deprecated(forRemoval = true)
  default VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, short id) {
    return this.createSimpleBlock(id, air, solid, motionBlocking);
  }

  /**
   * Creates new virtual customizable block
   *
   * @param blockStateId   Block protocol id
   * @param air            Defines if the block is the air
   * @param solid          Defines if the block is solid
   * @param motionBlocking Defines if the block blocks motions (1.14+)
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(short blockStateId, boolean air, boolean solid, boolean motionBlocking);

  @Deprecated(forRemoval = true)
  default VirtualBlock createSimpleBlock(boolean solid, boolean air, boolean motionBlocking, String modernId, Map<String, String> properties) {
    return this.createSimpleBlock(modernId, properties, air, solid, motionBlocking);
  }

  /**
   * Creates new virtual customizable block
   *
   * @param modernId       Block id
   * @param properties     Modern properties like {"waterlogged": "true"}
   * @param air            Defines if the block is the air
   * @param solid          Defines if the block is solid
   * @param motionBlocking Defines if the block blocks motions (1.14+)
   *
   * @return new virtual block
   */
  VirtualBlock createSimpleBlock(String modernId, Map<String, String> properties, boolean air, boolean solid, boolean motionBlocking);

  /**
   * Creates new virtual world
   *
   * @param dimension World dimension.
   * @param posX      Spawn location x
   * @param posY      Spawn location y
   * @param posZ      Spawn location z
   * @param yaw       Spawn rotation yaw
   * @param pitch     Spawn rotation pitch
   *
   * @return new virtual world
   */
  VirtualWorld createVirtualWorld(Dimension dimension, double posX, double posY, double posZ, float yaw, float pitch);

  /**
   * Creates new virtual chunk with plain biomes set as default
   * You need to provide the chunk location, you can get it using {@code blockCoordinate >> 4}
   *
   * @param posX Chunk position by X
   * @param posZ Chunk position by Z
   *
   * @return new virtual chunk
   */
  @Deprecated
  VirtualChunk createVirtualChunk(int posX, int posZ);

  /**
   * Creates new virtual chunk
   * You need to provide the chunk location, you can get it using {@code blockCoordinate >> 4}
   *
   * @param posX         Chunk position by X
   * @param posZ         Chunk position by Z
   * @param defaultBiome Default biome to fill it
   *
   * @return new virtual chunk.
   */
  VirtualChunk createVirtualChunk(int posX, int posZ, VirtualBiome defaultBiome);

  /**
   * Creates new virtual chunk
   * You need to provide the chunk location, you can get it using ({@code block_coordinate >> 4})
   *
   * @param posX         Chunk position by X
   * @param posZ         Chunk position by Z
   * @param defaultBiome Default biome to fill it
   *
   * @return new virtual chunk
   */
  VirtualChunk createVirtualChunk(int posX, int posZ, BuiltInBiome defaultBiome);

  /**
   * Creates new virtual server
   *
   * @param world Virtual world
   *
   * @return new virtual server
   */
  Limbo createLimbo(VirtualWorld world);


  /**
   * Releases a thread after PreparedPacket#build executions
   * Used to free compression libraries
   */
  void releasePreparedPacketThread(Thread thread);

  /**
   * Creates new prepared packet builder
   *
   * @return new prepared packet
   */
  PreparedPacket createPreparedPacket();

  /**
   * Creates new prepared packet builder
   *
   * @param minVersion Minimum version to prepare
   * @param maxVersion Maximum version to prepare
   *
   * @return new prepared packet
   */
  PreparedPacket createPreparedPacket(ProtocolVersion minVersion, ProtocolVersion maxVersion);

  /**
   * Creates new prepared packet builder for the CONFIG state
   *
   * @return new prepared packet
   */
  PreparedPacket createConfigPreparedPacket();

  /**
   * Creates new prepared packet builder for the CONFIG state
   *
   * @param minVersion Minimum version to prepare
   * @param maxVersion Maximum version to prepare
   *
   * @return new prepared packet
   */
  PreparedPacket createConfigPreparedPacket(ProtocolVersion minVersion, ProtocolVersion maxVersion);

  /**
   * Pass the player to the next Login Limbo, without spawning at current Limbo
   *
   * @param player Player to pass
   */
  void passLoginLimbo(Player player);

  /**
   * Creates new virtual item from Item enum
   *
   * @param item Item from item enum
   *
   * @return new virtual item
   */
  VirtualItem getItem(Item item);

  /**
   * Creates new virtual item from Item enum
   *
   * @param modernId Modern item identifier
   *
   * @return new virtual item
   */
  VirtualItem getItem(String modernId);

  /**
   * Creates new virtual item from Item enum
   *
   * @param legacyId Legacy item ID
   *
   * @return new virtual item
   */
  VirtualItem getLegacyItem(int legacyId);

  /**
   * @return new item component map
   */
  ItemComponentMap createItemComponentMap();

  @Deprecated(forRemoval = true)
  default VirtualBlockEntity getBlockEntity(String entityId) {
    return this.getBlockEntityFromModernId(entityId);
  }

  /**
   * @param modernId Should be prefixed with minecraft namespace
   */
  @Nullable
  VirtualBlockEntity getBlockEntityFromModernId(String modernId);

  /**
   * @return Block entity using legacy id or null (used in 1.9-1.10, e.g., DLDetector instead of daylight_detector)
   */
  @Nullable
  VirtualBlockEntity getBlockEntityFromLegacyId(String legacyId);

  /**
   * A factory to instantiate Minecraft packet objects
   */
  PacketFactory getPacketFactory();

  ProtocolVersion getPrepareMinVersion();

  ProtocolVersion getPrepareMaxVersion();

  /**
   * Opens world file (a.k.a. schematic file)
   *
   * @param apiType World file type
   * @param file World file
   * @return Ready to use WorldFile
   */
  WorldFile openWorldFile(BuiltInWorldFileType apiType, Path file) throws IOException;


  /**
   * Opens world file (a.k.a. schematic file)
   *
   * @param apiType World file type
   * @param stream World file stream
   * @return Ready to use WorldFile
   */
  WorldFile openWorldFile(BuiltInWorldFileType apiType, InputStream stream) throws IOException;


  /**
   * Opens world file (a.k.a. schematic file)
   *
   * @param apiType World file type
   * @param tag World file NBT tag
   * @return Ready to use WorldFile
   */
  WorldFile openWorldFile(BuiltInWorldFileType apiType, CompoundBinaryTag tag);
}
