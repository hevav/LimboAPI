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

package net.elytrium.limboapi.server.world;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimpleWorld implements VirtualWorld {

  private final Map<Long, SimpleChunk> chunks = new HashMap<>();
  private final List<List<VirtualChunk>> distanceChunkMap = new ArrayList<>();
  @NonNull
  private final Dimension dimension;
  private final VirtualBiome defaultBiome;

  private final double spawnX;
  private final double spawnY;
  private final double spawnZ;
  private final float yaw;
  private final float pitch;

  public SimpleWorld(@NonNull Dimension dimension, double posX, double posY, double posZ, float yaw, float pitch) {
    this.dimension = dimension;
    this.defaultBiome = Biome.of(dimension.getDefaultBiome());

    this.spawnX = posX;
    this.spawnY = posY;
    this.spawnZ = posZ;
    this.yaw = yaw;
    this.pitch = pitch;

    //this.getChunkOrNew((int) Math.floor(posX), (int) Math.floor(posZ));
  }

  @Override
  public void setBlockEntity(int posX, int posY, int posZ, @Nullable CompoundBinaryTag nbt, @Nullable VirtualBlockEntity blockEntity) {
    this.getChunkOrNew(posX, posZ).setBlockEntity(posX, posY, posZ, nbt, blockEntity);
  }

  @Override
  public void setBlock(int posX, int posY, int posZ, @Nullable VirtualBlock block) {
    this.getChunkOrNew(posX, posZ).setBlock(getChunkCoordinate(posX), posY, getChunkCoordinate(posZ), block);
  }

  @NonNull
  @Override
  public VirtualBlock getBlock(int posX, int posY, int posZ) {
    return this.chunkAction(posX, posZ, chunk -> chunk.getBlock(getChunkCoordinate(posX), posY, getChunkCoordinate(posZ)), () -> SimpleBlock.AIR);
  }

  @Override
  public void setBiome2d(int posX, int posZ, @NonNull VirtualBiome biome) {
    this.getChunkOrNew(posX, posZ).setBiome2D(getChunkCoordinate(posX), getChunkCoordinate(posZ), biome);
  }

  @Override
  public void setBiome3d(int posX, int posY, int posZ, @NonNull VirtualBiome biome) {
    this.getChunkOrNew(posX, posZ).setBiome3D(getChunkCoordinate(posX), posY, getChunkCoordinate(posZ), biome);
  }

  @Override
  public VirtualBiome getBiome(int posX, int posY, int posZ) {
    return this.chunkAction(posX, posZ, chunk -> chunk.getBiome(posX, posY, posZ), () -> Biome.PLAINS);
  }

  @Override
  public byte getBlockLight(int posX, int posY, int posZ) {
    return this.chunkAction(posX, posZ, chunk -> chunk.getBlockLight(getChunkCoordinate(posX), posY, getChunkCoordinate(posZ)), () -> (byte) 0);
  }

  @Override
  public void setBlockLight(int posX, int posY, int posZ, byte light) {
    this.getChunkOrNew(posX, posZ).setBlockLight(getChunkCoordinate(posX), posY, getChunkCoordinate(posZ), light);
  }

  @Override
  public void fillBlockLight(int level) {
    for (SimpleChunk chunk : this.chunks.values()) {
      chunk.fillBlockLight(level);
    }
  }

  @Override
  public void fillSkyLight(int level) {
    for (SimpleChunk chunk : this.chunks.values()) {
      chunk.fillSkyLight(level);
    }
  }

  @Override
  public List<VirtualChunk> getChunks() {
    return ImmutableList.copyOf(this.chunks.values());
  }

  @Override
  public List<List<VirtualChunk>> getOrderedChunks() {
    return this.distanceChunkMap.stream()
        .map(Collections::unmodifiableList)
        .toList();
  }

  private int getDistanceToSpawn(VirtualChunk chunk) {
    int diffX = getChunkXZ((int) this.spawnX) - chunk.getPosX();
    int diffZ = getChunkXZ((int) this.spawnZ) - chunk.getPosZ();
    return (int) Math.sqrt((diffX * diffX) + (diffZ * diffZ));
  }

  @Nullable
  @Override
  public SimpleChunk getChunk(int posX, int posZ) {
    return this.chunks.get(getChunkIndex(getChunkXZ(posX), getChunkXZ(posZ)));
  }

  @Override
  public SimpleChunk getChunkOrNew(int posX, int posZ) {
    posX = getChunkXZ(posX);
    posZ = getChunkXZ(posZ);

    // Modern Sodium versions don't load chunks if their "neighbors" are unloaded
    // We are fixing this problem there by generating all the "neighbors"
    for (int chunkX = posX - 1; chunkX <= posX + 1; ++chunkX) {
      for (int chunkZ = posZ - 1; chunkZ <= posZ + 1; ++chunkZ) {
        this.localCreateChunk(chunkX, chunkZ);
      }
    }

    return this.chunks.get(getChunkIndex(posX, posZ));
  }

  private void localCreateChunk(int posX, int posZ) {
    long index = getChunkIndex(posX, posZ);
    if (!this.chunks.containsKey(index)) {
      SimpleChunk chunk = new SimpleChunk(posX, posZ, this.defaultBiome);

      this.chunks.put(index, chunk);

      int distance = this.getDistanceToSpawn(chunk);
      for (int i = this.distanceChunkMap.size(); i <= distance; ++i) {
        this.distanceChunkMap.add(new LinkedList<>());
      }

      this.distanceChunkMap.get(distance).add(chunk);
    }
  }

  @NonNull
  @Override
  public Dimension getDimension() {
    return this.dimension;
  }

  @Override
  public double getSpawnX() {
    return this.spawnX;
  }

  @Override
  public double getSpawnY() {
    return this.spawnY;
  }

  @Override
  public double getSpawnZ() {
    return this.spawnZ;
  }

  @Override
  public float getYaw() {
    return this.yaw;
  }

  @Override
  public float getPitch() {
    return this.pitch;
  }

  private <T> T chunkAction(int posX, int posZ, Function<SimpleChunk, T> function, Supplier<T> ifNull) {
    SimpleChunk chunk = this.getChunk(posX, posZ);
    return chunk == null ? ifNull.get() : function.apply(chunk);
  }

  private static long getChunkIndex(int posX, int posZ) {
    return (long) posX << 32 | posZ & 0xFFFFFFFFL;
  }

  private static int getChunkXZ(int pos) {
    return pos >> 4;
  }

  private static int getChunkCoordinate(int pos) {
    return pos & 0x0F;
  }
}
