/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.material.WorldVersion;

public interface VirtualBlock {

  @Deprecated(forRemoval = true)
  default String getModernStringID() {
    return this.modernId();
  }

  String modernId();

  @Deprecated(forRemoval = true)
  default short getModernID() {
    return this.blockStateId();
  }

  /**
   * @return Latest supported version block state id
   */
  short blockStateId();

  @Deprecated(forRemoval = true)
  default short getID(ProtocolVersion version) {
    return this.getBlockStateID(version);
  }

  @Deprecated(forRemoval = true)
  default short getBlockStateID(ProtocolVersion version) {
    return this.blockStateId(version);
  }

  short blockStateId(ProtocolVersion version);

  /**
   * @return Latest supported version block id
   */
  short blockId();

  @Deprecated(forRemoval = true)
  default short getBlockID(WorldVersion version) {
    return this.blockId(version);
  }

  short blockId(WorldVersion version);

  @Deprecated(forRemoval = true)
  default short getBlockID(ProtocolVersion version) {
    return this.blockId(version);
  }

  short blockId(ProtocolVersion version);

  @Deprecated(forRemoval = true)
  default boolean isSolid() {
    return this.solid();
  }

  boolean solid();

  @Deprecated(forRemoval = true)
  default boolean isAir() {
    return this.air();
  }

  boolean air();

  @Deprecated(forRemoval = true)
  default boolean isMotionBlocking() {
    return this.motionBlocking();
  }

  boolean motionBlocking();

  boolean isSupportedOn(ProtocolVersion version);

  boolean isSupportedOn(WorldVersion version);
}
