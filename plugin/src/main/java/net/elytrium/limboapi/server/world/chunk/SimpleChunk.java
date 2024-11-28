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

package net.elytrium.limboapi.server.world.chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public class SimpleChunk implements VirtualChunk {

  public static final int MAX_BLOCKS_PER_SECTION = 16 * 16 * 16;
  public static final int MAX_BIOMES_PER_SECTION = 4 * 4 * 4;

  private final int posX;
  private final int posZ;

  private final SimpleSection[] sections = new SimpleSection[16];
  private final LightSection[] light = new LightSection[18];
  private final VirtualBiome[] biomes = new VirtualBiome[1024];
  private final List<VirtualBlockEntity.Entry> blockEntityEntries = new ArrayList<>();

  public SimpleChunk(int posX, int posZ) {
    this(posX, posZ, Biome.PLAINS);
  }

  public SimpleChunk(int posX, int posZ, VirtualBiome defaultBiome) {
    this.posX = posX;
    this.posZ = posZ;

    for (int i = 0; i < this.light.length; ++i) {
      this.light[i] = new SimpleLightSection();
    }

    Arrays.fill(this.biomes, defaultBiome);
  }

  @Override
  public void setBlock(int posX, int posY, int posZ, @Nullable VirtualBlock block) {
    this.getSection(posY).setBlockAt(posX, posY & 0x0F, posZ, block);
  }

  @Override
  public void setBlockEntity(int posX, int posY, int posZ, @Nullable CompoundBinaryTag nbt, @Nullable VirtualBlockEntity blockEntity) {
    this.blockEntityEntries.removeIf(entry -> entry.getPosX() == posX && entry.getPosY() == posY && entry.getPosZ() == posZ);
    if (blockEntity != null) {
      this.blockEntityEntries.add(blockEntity.createEntry(this, posX, posY, posZ, nbt));
    }
  }

  @Override
  public void setBlockEntity(VirtualBlockEntity.Entry blockEntityEntry) {
    this.blockEntityEntries.add(blockEntityEntry);
  }

  private SimpleSection getSection(int posY) {
    int sectionIndex = getSectionIndex(posY);
    SimpleSection section = this.sections[sectionIndex];
    if (section == null) {
      section = new SimpleSection();
      this.sections[sectionIndex] = section;
    }

    return section;
  }

  @NonNull
  @Override
  public VirtualBlock getBlock(int posX, int posY, int posZ) {
    SimpleSection section = this.sections[getSectionIndex(posY)];
    return section == null ? SimpleBlock.AIR : section.getBlockAt(posX, posY & 0x0F, posZ);
  }

  @Override
  public void setBiome2D(int posX, int posZ, @NonNull VirtualBiome biome) {
    for (int posY = 0; posY < 256; posY += 4) {
      this.setBiome3D(posX, posY, posZ, biome);
    }
  }

  @Override
  public void setBiome3D(int posX, int posY, int posZ, @NonNull VirtualBiome biome) {
    this.biomes[getBiomeIndex(posX, posY, posZ)] = biome;
  }

  @NonNull
  @Override
  public VirtualBiome getBiome(int posX, int posY, int posZ) {
    return this.biomes[getBiomeIndex(posX, posY, posZ)];
  }

  @Override
  public void setBlockLight(int posX, int posY, int posZ, byte light) {
    this.getLightSection(posY).setBlockLight(posX, posY & 0x0F, posZ, light);
  }

  @Override
  public byte getBlockLight(int posX, int posY, int posZ) {
    return this.getLightSection(posY).getBlockLight(posX, posY & 0x0F, posZ);
  }

  @Override
  public void setSkyLight(int posX, int posY, int posZ, byte light) {
    this.getLightSection(posY).setSkyLight(posX, posY & 0x0F, posZ, light);
  }

  @Override
  public byte getSkyLight(int posX, int posY, int posZ) {
    return this.getLightSection(posY).getSkyLight(posX, posY & 0x0F, posZ);
  }

  private LightSection getLightSection(int posY) {
    return this.light[posY < 0 ? 0 : getSectionIndex(posY) + 1];
  }

  @Override
  public void fillBlockLight(@IntRange(from = 0, to = 15) int level) {
    for (LightSection lightSection : this.light) {
      lightSection.getBlockLight().fill(level);
    }
  }

  @Override
  public void fillSkyLight(@IntRange(from = 0, to = 15) int level) {
    for (LightSection lightSection : this.light) {
      lightSection.getSkyLight().fill(level);
    }
  }

  @Override
  public int getPosX() {
    return this.posX;
  }

  @Override
  public int getPosZ() {
    return this.posZ;
  }

  @Override
  public ChunkSnapshot createSnapshot(boolean full) {
    return new SimpleChunkSnapshot(this.posX, this.posZ, full, this.createBlockSectionSnapshot(), this.createLightSnapshot(), this.biomes.clone(), this.blockEntityEntries.toArray(VirtualBlockEntity.Entry[]::new));
  }

  @Override
  public BlockSection[] createBlockSectionSnapshot() {
    SimpleSection[] snapshot = new SimpleSection[this.sections.length];
    for (int i = 0; i < snapshot.length; ++i) {
      SimpleSection section = this.sections[i];
      if (section != null) {
        snapshot[i] = section.copy();
      }
    }

    return snapshot;
  }

  @Override
  public LightSection[] createLightSnapshot() {
    LightSection[] snapshot = new LightSection[this.light.length];
    for (int i = 0; i < snapshot.length; ++i) {
      snapshot[i] = this.light[i].copy();
    }

    return snapshot;
  }

  private static int getBiomeIndex(int posX, int posY, int posZ) {
    return (posY >> 2 & 0b0000111111) << 4 | (posZ >> 2 & 0b11) << 2 | posX >> 2 & 0b11;
  }

  private static int getSectionIndex(int posY) {
    return posY >> 4;
  }

  @Override
  public String toString() {
    return "SimpleChunk{"
        + "posX=" + this.posX
        + ", posZ=" + this.posZ
        + '}';
  }
}
