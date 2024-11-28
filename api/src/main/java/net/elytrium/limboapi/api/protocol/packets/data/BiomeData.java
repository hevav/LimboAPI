/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

import java.util.HashMap;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;

/**
 * For ChunkData packet
 */
public class BiomeData {

  private final int[] post115Biomes = new int[1024];
  private final int[] pre115Biomes = new int[256];

  public BiomeData(ChunkSnapshot chunk) {
    VirtualBiome[] biomes = chunk.biomes();
    for (int i = 0; i < biomes.length; ++i) {
      this.post115Biomes[i] = biomes[i].getId();
    }

    // Down sample 4x4x4 3D biomes to 2D XZ
    Map<Integer, Integer> samples = new HashMap<>(64);
    for (int posX = 0; posX < 16; posX += 4) {
      for (int posZ = 0; posZ < 16; posZ += 4) {
        samples.clear();
        for (int posY = 0; posY < 256; posY += 16) {
          VirtualBiome biome = biomes[/*SimpleChunk.getBiomeIndex(posX, posY, posZ)*/(posY >> 2 & 63) << 4 | (posZ >> 2 & 3) << 2 | posX >> 2 & 3];
          samples.put(biome.getId(), samples.getOrDefault(biome.getId(), 0) + 1);
        }
        int id = samples.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .orElseThrow()
            .getKey();
        for (int i = posX; i < posX + 4; ++i) {
          for (int j = posZ; j < posZ + 4; ++j) {
            this.pre115Biomes[(j << 4) + i] = id;
          }
        }
      }
    }
  }

  public int[] getPost115Biomes() {
    return this.post115Biomes;
  }

  public int[] getPre115Biomes() {
    return this.pre115Biomes;
  }
}
