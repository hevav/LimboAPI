/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk.data;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface BlockStorage {

  /**
   * @param pass Used only in 1.7-1.8 storage
   */
  void write(Object buf, ProtocolVersion version, int pass);

  void set(int posX, int posY, int posZ, @NonNull VirtualBlock block);

  @NonNull
  VirtualBlock get(int posX, int posY, int posZ);

  int getDataLength(ProtocolVersion version);

  BlockStorage copy();

  static int index(int posX, int posY, int posZ) {
    return posY << 8 | posZ << 4 | posX;
  }
}
