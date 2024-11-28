/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

public enum Dimension {

  OVERWORLD("minecraft:overworld", 0, 0, 28, true, BuiltInBiome.PLAINS), // (384 + 64) / 16
  NETHER("minecraft:the_nether", -1, 1, 16, false, BuiltInBiome.NETHER_WASTES), // 256 / 16
  THE_END("minecraft:the_end", 1, 2, 16, false, BuiltInBiome.THE_END); // 256 / 16

  private final String key;
  private final int legacyId;
  private final int modernId;
  private final int maxSections;
  private final boolean hasSkyLight;
  private final BuiltInBiome defaultBiome;

  Dimension(String key, int legacyId, int modernId, int maxSections, boolean hasSkyLight, BuiltInBiome defaultBiome) {
    this.key = key;
    this.legacyId = legacyId;
    this.modernId = modernId;
    this.maxSections = maxSections;
    this.hasSkyLight = hasSkyLight;
    this.defaultBiome = defaultBiome;
  }

  public String getKey() {
    return this.key;
  }

  @Deprecated(forRemoval = true)
  public int getLegacyID() {
    return this.legacyId;
  }

  public int getLegacyId() {
    return this.legacyId;
  }

  @Deprecated(forRemoval = true)
  public int getModernID() {
    return this.modernId;
  }

  public int getModernId() {
    return this.modernId;
  }

  public int getMaxSections() {
    return this.maxSections;
  }

  @Deprecated(forRemoval = true)
  public boolean hasLegacySkyLight() {
    return this.hasSkyLight;
  }

  public boolean hasSkyLight() {
    return this.hasSkyLight;
  }

  public BuiltInBiome getDefaultBiome() {
    return this.defaultBiome;
  }
}

