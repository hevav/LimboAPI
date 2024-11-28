/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;

public interface ChunkSnapshot {

  VirtualBlock getBlock(int posX, int posY, int posZ);

  @Deprecated(forRemoval = true)
  default int getPosX() {
    return this.posX();
  }

  int posX();

  @Deprecated(forRemoval = true)
  default int getPosZ() {
    return this.posZ();
  }

  int posZ();

  @Deprecated(forRemoval = true)
  default boolean isFullChunk() {
    return this.fullChunk();
  }

  boolean fullChunk();

  @Deprecated(forRemoval = true)
  default BlockSection[] getSections() {
    return this.sections();
  }

  BlockSection[] sections();

  @Deprecated(forRemoval = true)
  default LightSection[] getLight() {
    return this.light();
  }

  LightSection[] light();

  @Deprecated(forRemoval = true)
  default VirtualBiome[] getBiomes() {
    return this.biomes();
  }

  VirtualBiome[] biomes();

  @Deprecated(forRemoval = true)
  default List<VirtualBlockEntity.Entry> getBlockEntityEntries() {
    return List.of(this.blockEntityEntries());
  }

  default VirtualBlockEntity.Entry[] blockEntityEntries(ProtocolVersion version) {
    return version.lessThan(ProtocolVersion.MINECRAFT_1_17)
        ? this.blockEntityEntriesStream(version).toArray(VirtualBlockEntity.Entry[]::new)
        : this.blockEntityEntries();
  }

  default Stream<VirtualBlockEntity.Entry> blockEntityEntriesStream(ProtocolVersion version) {
    var stream = Arrays.stream(this.blockEntityEntries());
    return version.lessThan(ProtocolVersion.MINECRAFT_1_17)
        ? stream.filter(entry -> entry.getPosY() >= 0 && entry.getPosY() <= 255)
        : stream;
  }

  VirtualBlockEntity.Entry[] blockEntityEntries();
}
