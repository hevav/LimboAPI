/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.chunk;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public enum BlockEntityVersion {

  LEGACY(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_9_2)),
  MINECRAFT_1_9(EnumSet.range(ProtocolVersion.MINECRAFT_1_9_4, ProtocolVersion.MINECRAFT_1_10)),
  MINECRAFT_1_11(EnumSet.of(ProtocolVersion.MINECRAFT_1_11, ProtocolVersion.MINECRAFT_1_11_1)),
  MINECRAFT_1_12(EnumSet.range(ProtocolVersion.MINECRAFT_1_12, ProtocolVersion.MINECRAFT_1_12_2)),
  MINECRAFT_1_13(EnumSet.range(ProtocolVersion.MINECRAFT_1_13, ProtocolVersion.MINECRAFT_1_13_2)),
  MINECRAFT_1_14(EnumSet.range(ProtocolVersion.MINECRAFT_1_14, ProtocolVersion.MINECRAFT_1_14_4)),
  MINECRAFT_1_15(EnumSet.range(ProtocolVersion.MINECRAFT_1_15, ProtocolVersion.MINECRAFT_1_16_4)),
  MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MINECRAFT_1_18_2)),
  MINECRAFT_1_19(EnumSet.range(ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MINECRAFT_1_19_1)),
  MINECRAFT_1_19_3(EnumSet.of(ProtocolVersion.MINECRAFT_1_19_3)),
  MINECRAFT_1_19_4(EnumSet.range(ProtocolVersion.MINECRAFT_1_19_4, ProtocolVersion.MINECRAFT_1_20)),
  MINECRAFT_1_20(EnumSet.range(ProtocolVersion.MINECRAFT_1_20, ProtocolVersion.MINECRAFT_1_20_2)),
  MINECRAFT_1_20_3(EnumSet.of(ProtocolVersion.MINECRAFT_1_20_3)),
  MINECRAFT_1_20_5(EnumSet.range(ProtocolVersion.MINECRAFT_1_20_5, ProtocolVersion.MINECRAFT_1_21)),
  MINECRAFT_1_21_2(EnumSet.range(ProtocolVersion.MINECRAFT_1_21_2, ProtocolVersion.MAXIMUM_VERSION));

  private static final EnumMap<ProtocolVersion, BlockEntityVersion> MC_VERSION_TO_ITEM_VERSIONS = new EnumMap<>(ProtocolVersion.class);

  private final Set<ProtocolVersion> versions;

  BlockEntityVersion(Set<ProtocolVersion> versions) {
    this.versions = versions;
  }

  public Set<ProtocolVersion> getVersions() {
    return this.versions;
  }

  static {
    for (BlockEntityVersion version : BlockEntityVersion.values()) {
      for (ProtocolVersion protocolVersion : version.getVersions()) {
        MC_VERSION_TO_ITEM_VERSIONS.put(protocolVersion, version);
      }
    }
  }

  public static BlockEntityVersion from(ProtocolVersion protocolVersion) {
    return MC_VERSION_TO_ITEM_VERSIONS.get(protocolVersion);
  }
}
