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

package net.elytrium.limboapi.server.item;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.elytrium.limboapi.api.protocol.item.DataComponentType;
import net.elytrium.limboapi.api.protocol.item.ItemComponent;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;

@SuppressWarnings("unchecked")
public class SimpleItemComponentMap implements ItemComponentMap {

  private Map<DataComponentType, AbstractItemComponent<?>> components;

  @Override
  public <T> ItemComponentMap put(DataComponentType type, T value) {
    if (this.components == null) {
      this.components = new HashMap<>();
    }

    this.components.put(type, ItemComponentRegistry.createComponent(type, value));
    return this;
  }

  @Override
  public <T> ItemComponent<T> remove(DataComponentType type) {
    if (this.components == null) {
      this.components = new HashMap<>();
    }

    return (ItemComponent<T>) this.components.put(type, null);
  }

  @Override
  public <T> T get(DataComponentType type) {
    if (this.components == null || this.components.isEmpty()) {
      return null;
    }

    AbstractItemComponent<?> component = this.components.get(type);
    return component == null ? null : (T) component.getValue();
  }

  @Override
  public <T> T getOrDefault(DataComponentType type, T defaultValue) {
    if (this.components == null || this.components.isEmpty()) {
      return defaultValue;
    }

    T value = this.get(type);
    return value == null ? defaultValue : value;
  }

  @Override
  public Map<DataComponentType, ItemComponent<?>> getComponents() {
    if (this.components == null) {
      this.components = new HashMap<>();
    }

    return (Map<DataComponentType, ItemComponent<?>>) (Object) this.components;
  }

  @Override
  public ItemComponentMap read(Object bufObj, ProtocolVersion version) {
    ByteBuf buf = (ByteBuf) bufObj;

    int added = ProtocolUtils.readVarInt(buf);
    int removed = ProtocolUtils.readVarInt(buf);
    if (added != 0 || removed != 0) {
      this.read(buf, version, added, removed);
    }

    return this;
  }

  private void read(ByteBuf buf, ProtocolVersion version, int added, int removed) {
    if (this.components == null) {
      this.components = new HashMap<>(added + removed);
    }

    for (int i = 0; i < added; ++i) {
      DataComponentType type = ItemComponentRegistry.getType(ProtocolUtils.readVarInt(buf), version);
      this.components.put(type, ItemComponentRegistry.createComponent(type, buf, version));
    }

    for (int i = 0; i < removed; ++i) {
      this.components.put(ItemComponentRegistry.getType(ProtocolUtils.readVarInt(buf), version), null);
    }
  }

  @Override
  public ItemComponentMap write(Object bufObj, ProtocolVersion version) {
    ByteBuf buf = (ByteBuf) bufObj;

    if (this.components == null || this.components.isEmpty()) {
      ProtocolUtils.writeVarInt(buf, 0);
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      int added = 0;
      int removed = 0;
      for (AbstractItemComponent<?> component : this.components.values()) {
        if (component.getValue() == null) {
          ++removed;
        } else {
          ++added;
        }
      }

      ProtocolUtils.writeVarInt(buf, added);
      ProtocolUtils.writeVarInt(buf, removed);

      this.components.forEach((key, component) -> {
        if (component != null && component.getValue() != null) {
          int id = ItemComponentRegistry.getId(key, version);
          if (id != Integer.MIN_VALUE) { // allow plugins to send version-specific components and don't do throw if id cannot be found
            ProtocolUtils.writeVarInt(buf, id);
            component.write(buf, version);
          }
        }
      });

      this.components.forEach((key, component) -> {
        if (component == null || component.getValue() == null) {
          int id = ItemComponentRegistry.getId(key, version);
          if (id != Integer.MIN_VALUE) {
            ProtocolUtils.writeVarInt(buf, id);
          }
        }
      });
    }

    return this;
  }

  @Override
  public String toString() {
    return "SimpleItemComponentMap{"
        + "components=" + this.components
        + "}";
  }

  public static ItemComponentMap read(ByteBuf buf, ProtocolVersion version) {
    int added = ProtocolUtils.readVarInt(buf);
    int removed = ProtocolUtils.readVarInt(buf);
    if (added == 0 && removed == 0) {
      return null;
    } else {
      SimpleItemComponentMap map = new SimpleItemComponentMap();
      map.read(buf, version, added, removed);
      return map;
    }
  }
}
