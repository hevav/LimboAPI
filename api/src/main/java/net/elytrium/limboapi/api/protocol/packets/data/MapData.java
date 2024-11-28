/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

/**
 * For MapData packet
 */
public record MapData(int columns, int rows, int posX, int posY, byte[] data) {

  public static final int MAP_DIM_SIZE = 128;
  public static final int MAP_SIZE = MAP_DIM_SIZE * MAP_DIM_SIZE; // 128² == 16384

  public MapData(byte[] data) {
    this(0, data);
  }

  public MapData(int posX, byte[] data) {
    this(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, posX, 0, data);
  }

  @Deprecated(forRemoval = true)
  public int getColumns() {
    return this.columns;
  }

  @Deprecated(forRemoval = true)
  public int getRows() {
    return this.rows;
  }

  @Deprecated(forRemoval = true)
  public int getX() {
    return this.posX;
  }

  @Deprecated(forRemoval = true)
  public int getY() {
    return this.posY;
  }

  @Deprecated(forRemoval = true)
  public byte[] getData() {
    return this.data;
  }
}
