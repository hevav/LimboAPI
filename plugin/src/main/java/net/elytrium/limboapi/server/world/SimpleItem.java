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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.utils.JsonParser;

public class SimpleItem implements VirtualItem {

  private static final Map<String, SimpleItem> MODERN_ID_MAP;
  private static final Map<Integer, SimpleItem> LEGACY_ID_MAP;

  private final Map<WorldVersion, Short> versionIds = new EnumMap<>(WorldVersion.class);
  private final String modernId;

  public SimpleItem(String modernId) {
    this.modernId = modernId;
  }

  public String modernId() {
    return this.modernId;
  }

  @Override
  public short itemId(WorldVersion version) {
    return this.versionIds.get(version);
  }

  @Override
  public short itemId(ProtocolVersion version) {
    return this.itemId(WorldVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(WorldVersion version) {
    return this.versionIds.containsKey(version);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.isSupportedOn(WorldVersion.from(version));
  }

  static {
    LinkedTreeMap<String, LinkedTreeMap<String, Number>> itemsMapping = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/items_mappings.json"));
    MODERN_ID_MAP = new HashMap<>(itemsMapping.size());
    LinkedTreeMap<String, String> modernIdRemap = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/modern_item_id_remap.json"));
    itemsMapping.forEach((modernId, versions) -> {
      SimpleItem simpleItem = new SimpleItem(modernId);
      versions.forEach((version, item) -> simpleItem.versionIds.put(WorldVersion.from(ProtocolVersion.getProtocolVersion(Integer.parseInt(version))), item.shortValue()));
      SimpleItem.MODERN_ID_MAP.put(modernId, simpleItem);

      String remapped = modernIdRemap.get(modernId);
      if (remapped != null) {
        if (SimpleItem.MODERN_ID_MAP.containsKey(remapped)) {
          throw new IllegalStateException("Remapped id " + remapped + " (from " + modernId + ") already exists");
        }

        SimpleItem.MODERN_ID_MAP.put(remapped, simpleItem);
      }
    });

    LinkedTreeMap<String, String> legacyItems = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/legacy_items.json"));
    LEGACY_ID_MAP = new HashMap<>(legacyItems.size());
    legacyItems.forEach((legacyProtocolId, modernId) -> {
      int id = Integer.parseInt(legacyProtocolId);
      SimpleItem value = SimpleItem.MODERN_ID_MAP.get(modernId);
      SimpleItem.LEGACY_ID_MAP.put(id, value);
      if (value != null) {
        value.versionIds.put(WorldVersion.LEGACY, (short) id);
      }
    });
  }

  public static SimpleItem fromItem(Item item) {
    return SimpleItem.LEGACY_ID_MAP.get(item.getLegacyId());
  }

  public static SimpleItem fromLegacyId(int id) {
    return SimpleItem.LEGACY_ID_MAP.get(id);
  }

  public static SimpleItem fromModernId(String id) {
    return SimpleItem.MODERN_ID_MAP.get(id);
  }
}
