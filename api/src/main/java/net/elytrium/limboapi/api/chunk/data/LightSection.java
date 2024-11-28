/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3D;

public interface LightSection {

  void setBlockLight(int posX, int posY, int posZ, byte light);

  NibbleArray3D getBlockLight();

  byte getBlockLight(int posX, int posY, int posZ);

  void setSkyLight(int posX, int posY, int posZ, byte light);

  NibbleArray3D getSkyLight();

  byte getSkyLight(int posX, int posY, int posZ);

  @Deprecated(forRemoval = true)
  default long getLastUpdate() {
    return 0;
  }

  LightSection copy();
}
