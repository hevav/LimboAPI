/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import net.elytrium.limboapi.api.chunk.VirtualBlock;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface BlockSection {

  void setBlockAt(int posX, int posY, int posZ, @Nullable VirtualBlock block);

  VirtualBlock getBlockAt(int posX, int posY, int posZ);

  @Deprecated(forRemoval = true)
  default BlockSection getSnapshot() {
    return this.copy();
  }

  BlockSection copy();

  @Deprecated(forRemoval = true)
  default long getLastUpdate() {
    return 0;
  }
}
