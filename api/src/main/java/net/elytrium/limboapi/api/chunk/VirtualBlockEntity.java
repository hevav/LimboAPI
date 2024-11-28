/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import com.velocitypowered.api.network.ProtocolVersion;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface VirtualBlockEntity {

  @Deprecated(forRemoval = true)
  default String getModernID() {
    return this.getModernId();
  }

  String getModernId();

  /**
   * @return Legacy id if present, modern otherwise (used in 1.9-1.10, e.g., DLDetector instead of daylight_detector)
   */
  String getLegacyId();

  @Deprecated(forRemoval = true)
  default int getID(ProtocolVersion version) {
    return this.getId(version);
  }

  /**
   * @return Protocol id, or {@link Integer#MIN_VALUE} if not exists
   */
  int getId(ProtocolVersion version);

  @Deprecated(forRemoval = true)
  default int getID(BlockEntityVersion version) {
    return this.getId(version);
  }

  /**
   * @return Protocol id, or {@link Integer#MIN_VALUE} if not exists
   */
  int getId(BlockEntityVersion version);

  boolean isSupportedOn(ProtocolVersion version);

  boolean isSupportedOn(BlockEntityVersion version);

  @Deprecated(forRemoval = true)
  default Entry getEntry(int posX, int posY, int posZ, CompoundBinaryTag nbt) {
    return this.createEntry(null, posX, posY, posZ, nbt);
  }

  /**
   * @param chunk Used only to get blockId for correct transformation some of <=1.12.2 block entities
   */
  Entry createEntry(@Nullable VirtualChunk chunk, int posX, int posY, int posZ, CompoundBinaryTag nbt);

  interface Entry {

    VirtualBlockEntity getBlockEntity();

    @Deprecated(forRemoval = true)
    default int getID(ProtocolVersion version) {
      return this.getBlockEntity().getId(version);
    }

    @Deprecated(forRemoval = true)
    default int getID(BlockEntityVersion version) {
      return this.getBlockEntity().getId(version);
    }

    @Deprecated(forRemoval = true)
    default boolean isSupportedOn(ProtocolVersion version) {
      return this.getBlockEntity().isSupportedOn(version);
    }

    @Deprecated(forRemoval = true)
    default boolean isSupportedOn(BlockEntityVersion version) {
      return this.getBlockEntity().isSupportedOn(version);
    }

    int getPosX();

    int getPosY();

    int getPosZ();

    CompoundBinaryTag getNbt(ProtocolVersion version);
  }
}
