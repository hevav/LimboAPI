/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.server.world;

import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.util.collection.ShortObjectHashMap;
import io.netty.util.collection.ShortObjectMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.utils.JsonParser;
import org.checkerframework.checker.nullness.qual.NonNull;

public record SimpleBlock(String modernId, short blockId, short blockStateId, boolean air, boolean solid, boolean motionBlocking/*1.14+*/) implements VirtualBlock {

  private static final ShortObjectHashMap<String> MODERN_BLOCK_STATE_PROTOCOL_ID_MAP;
  private static final Map<String, Map<Set<String>, Short>> MODERN_BLOCK_STATE_STRING_MAP;
  private static final EnumMap<ProtocolVersion, ShortObjectMap<Short>> MODERN_BLOCK_STATE_IDS_MAP;
  private static final Map<String, Short> MODERN_BLOCK_STRING_MAP;
  private static final ShortObjectHashMap<Map<WorldVersion, Short>> LEGACY_BLOCK_IDS_MAP;
  private static final ShortObjectHashMap<SimpleBlock> LEGACY_BLOCK_STATE_IDS_MAP;
  private static final Map<String, Map<String, String>> DEFAULT_PROPERTIES_MAP;
  private static final Map<String, String> MODERN_ID_REMAP;

  public static final SimpleBlock AIR = new SimpleBlock("minecraft:air", (short) 0, (short) 0, true, false, false);

  public SimpleBlock(String modernId, Map<String, String> properties, boolean air, boolean solid, boolean motionBlocking) {
    this(modernId, SimpleBlock.transformId(modernId, properties), air, solid, motionBlocking);
  }

  public SimpleBlock(short blockStateId, boolean air, boolean solid, boolean motionBlocking) {
    this(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, air, solid, motionBlocking);
  }

  public SimpleBlock(String modernId, short blockStateId, boolean air, boolean solid, boolean motionBlocking) {
    this(modernId, SimpleBlock.MODERN_BLOCK_STRING_MAP.get(modernId.indexOf('[') == -1 ? modernId : modernId.substring(0, modernId.indexOf('['))), blockStateId, air, solid, motionBlocking);
  }

  public SimpleBlock(String modernID, short blockId, Map<String, String> properties, boolean air, boolean solid, boolean motionBlocking) {
    this(modernID, blockId, SimpleBlock.transformId(modernID, properties), air, solid, motionBlocking);
  }

  public SimpleBlock(SimpleBlock block) {
    this(block.modernId, block.blockId, block.blockStateId, block.air, block.solid, block.motionBlocking);
  }

  @Override
  public short blockStateId(ProtocolVersion version) {
    return SimpleBlock.MODERN_BLOCK_STATE_IDS_MAP.get(version).getOrDefault(this.blockStateId, this.blockStateId);
  }

  @Override
  public short blockId(WorldVersion version) {
    return SimpleBlock.LEGACY_BLOCK_IDS_MAP.get(this.blockId).getOrDefault(version, this.blockId);
  }

  @Override
  public short blockId(ProtocolVersion version) {
    return this.blockId(WorldVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(WorldVersion version) {
    return SimpleBlock.LEGACY_BLOCK_IDS_MAP.get(this.blockId).containsKey(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.isSupportedOn(WorldVersion.from(version));
  }

  static {
    LinkedTreeMap<String, Number> blockStates = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/block_states.json"));
    MODERN_BLOCK_STATE_PROTOCOL_ID_MAP = new ShortObjectHashMap<>(blockStates.size());
    MODERN_BLOCK_STATE_STRING_MAP = new HashMap<>(blockStates.size());
    blockStates.forEach((blockState, number) -> {
      short value = number.shortValue();
      SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.put(value, blockState);
      int index = blockState.indexOf('[');
      if (index == -1) {
        SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.computeIfAbsent(blockState, key -> new HashMap<>()).put(null, value);
      } else {
        HashSet<String> params = new HashSet<>();
        Collections.addAll(params, blockState.substring(index + 1, blockState.length() - 1).split(","));
        SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.computeIfAbsent(blockState.substring(0, index), key -> new HashMap<>()).put(params, value);
      }
    });

    LinkedTreeMap<String, LinkedTreeMap<String, Number>> blocksMappings = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/blocks_mappings.json"));
    MODERN_BLOCK_STRING_MAP = new HashMap<>(blocksMappings.size());
    LEGACY_BLOCK_IDS_MAP = new ShortObjectHashMap<>(blocksMappings.size());
    String maximumVersionProtocol = Integer.toString(ProtocolVersion.MAXIMUM_VERSION.getProtocol());
    blocksMappings.forEach((modernId, versions) -> {
      short modernProtocolId = versions.get(maximumVersionProtocol).shortValue();
      SimpleBlock.MODERN_BLOCK_STRING_MAP.put(modernId, modernProtocolId);
      EnumMap<WorldVersion, Short> deserializedVersionMap = new EnumMap<>(WorldVersion.class);
      versions.forEach((version, id) -> deserializedVersionMap.put(WorldVersion.from(ProtocolVersion.getProtocolVersion(Integer.parseInt(version))), id.shortValue()));
      SimpleBlock.LEGACY_BLOCK_IDS_MAP.put(modernProtocolId, deserializedVersionMap);
    });

    LinkedTreeMap<String, LinkedTreeMap<String, Number>> blockStatesMappings = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/block_states_mappings.json"));
    MODERN_BLOCK_STATE_IDS_MAP = new EnumMap<>(ProtocolVersion.class);
    blockStatesMappings.forEach((modernId, versions) -> {
      Short id = null;
      for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
        id = versions.getOrDefault(Integer.toString(version.getProtocol()), id).shortValue();
        SimpleBlock.MODERN_BLOCK_STATE_IDS_MAP.computeIfAbsent(version, key -> new ShortObjectHashMap<>()).put(Short.parseShort(modernId), id);
      }
    });

    LinkedTreeMap<String, LinkedTreeMap<String, String>> properties = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/default_block_properties.json"));
    DEFAULT_PROPERTIES_MAP = new HashMap<>(properties.size());
    properties.forEach((key, value) -> SimpleBlock.DEFAULT_PROPERTIES_MAP.put(key, new HashMap<>(value)));

    MODERN_ID_REMAP = new HashMap<>(JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/modern_block_id_remap.json")));

    LinkedTreeMap<String, Number> legacyBlocks = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/legacy_blocks.json"));
    LEGACY_BLOCK_STATE_IDS_MAP = new ShortObjectHashMap<>(legacyBlocks.size() + 1);
    legacyBlocks.forEach((legacy, modern) -> SimpleBlock.LEGACY_BLOCK_STATE_IDS_MAP.put(Short.valueOf(legacy), SimpleBlock.solid(modern.shortValue())));
    SimpleBlock.LEGACY_BLOCK_STATE_IDS_MAP.put((short) 0, SimpleBlock.AIR);
  }

  public static VirtualBlock fromModernId(String modernId) {
    String[] deserializedModernId = modernId.split("[\\[\\]]");
    if (deserializedModernId.length < 2) {
      return SimpleBlock.fromModernId(modernId, Map.of());
    } else {
      Map<String, String> properties = new HashMap<>();
      for (String property : deserializedModernId[1].split(",")) {
        String[] propertyKeyValue = property.split("=");
        properties.put(propertyKeyValue[0], propertyKeyValue[1]);
      }

      return SimpleBlock.fromModernId(deserializedModernId[0], properties);
    }
  }

  public static VirtualBlock fromModernId(String modernId, Map<String, String> properties) {
    modernId = SimpleBlock.remapModernId(modernId);
    return SimpleBlock.solid(modernId, SimpleBlock.transformId(modernId, properties));
  }

  private static short transformId(String modernId, Map<String, String> properties) {
    Map<String, String> defaultProperties = SimpleBlock.DEFAULT_PROPERTIES_MAP.get(modernId);
    if (defaultProperties == null || defaultProperties.isEmpty()) {
      return SimpleBlock.transformId(modernId, (Set<String>) null);
    } else {
      Set<String> propertiesSet = new HashSet<>();
      defaultProperties.forEach((key, value) -> {
        if (properties != null) {
          value = properties.getOrDefault(key, value);
        }

        propertiesSet.add(key + "=" + value.toLowerCase(Locale.ROOT));
      });
      return transformId(modernId, propertiesSet);
    }
  }

  private static short transformId(String modernId, Set<String> properties) {
    Map<Set<String>, Short> blockInfo = SimpleBlock.MODERN_BLOCK_STATE_STRING_MAP.get(modernId);
    if (blockInfo == null) {
      LimboAPI.getLogger().warn("Block {} is not supported, and was replaced with air.", modernId);
      return SimpleBlock.AIR.blockStateId;
    }

    Short id = properties == null || properties.isEmpty() ? blockInfo.get(null) : blockInfo.get(properties);
    if (id == null) {
      LimboAPI.getLogger().warn("Block {} is not supported with {} properties, and was replaced with air.", modernId, properties);
      return SimpleBlock.AIR.blockStateId;
    }

    return id;
  }

  @NonNull
  public static SimpleBlock solid(short blockStateId) {
    return solid(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock solid(String modernId, short blockStateId) {
    return solid(SimpleBlock.remapModernId(modernId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock solid(short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, false, true, motionBlocking);
  }

  @NonNull
  public static SimpleBlock solid(String modernId, short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.remapModernId(modernId), blockStateId, false, true, motionBlocking);
  }

  @NonNull
  public static SimpleBlock nonSolid(short blockStateId) {
    return nonSolid(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock nonSolid(String modernId, short blockStateId) {
    return nonSolid(SimpleBlock.remapModernId(modernId), blockStateId, true);
  }

  @NonNull
  public static SimpleBlock nonSolid(short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.MODERN_BLOCK_STATE_PROTOCOL_ID_MAP.get(blockStateId), blockStateId, false, false, motionBlocking);
  }

  @NonNull
  public static SimpleBlock nonSolid(String modernId, short blockStateId, boolean motionBlocking) {
    return new SimpleBlock(SimpleBlock.remapModernId(modernId), blockStateId, false, false, motionBlocking);
  }

  @NonNull
  public static SimpleBlock fromLegacyId(short legacyId) {
    SimpleBlock block = SimpleBlock.LEGACY_BLOCK_STATE_IDS_MAP.get(legacyId);
    if (block == null) {
      LimboAPI.getLogger().warn("Block #{} is not supported, and was replaced with air", legacyId);
      return SimpleBlock.AIR;
    } else {
      return block;
    }
  }

  private static String remapModernId(String modernId) {
    int index = modernId.indexOf('[');
    String remappedId = SimpleBlock.MODERN_ID_REMAP.get(index == -1 ? modernId : modernId.substring(0, index));
    return remappedId == null
        ? modernId
        : index == -1
            ? remappedId
            : remappedId + modernId.substring(index);
  }
}
