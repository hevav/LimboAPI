/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

public interface VirtualBiome {

  String getName();

  @Deprecated(forRemoval = true)
  default int getID() {
    return this.getId();
  }

  int getId();
}
