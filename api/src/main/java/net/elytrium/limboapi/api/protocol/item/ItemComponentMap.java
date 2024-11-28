/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.item;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @sinceMinecraft 1.20.5
 */
public interface ItemComponentMap {

  @Deprecated(forRemoval = true)
  default <T> ItemComponentMap add(ProtocolVersion version, String name, T value) {
    return this.put(DataComponentType.valueOf(name.substring(10/*"minecraft:".length()*/).toUpperCase()), value);
  }

  @Deprecated(forRemoval = true)
  default ItemComponentMap remove(ProtocolVersion version, String name) {
    this.remove(DataComponentType.valueOf(name.substring(10/*"minecraft:".length()*/).toUpperCase()));
    return this;
  }

  <T> ItemComponentMap put(DataComponentType type, T value);

  <T> ItemComponent<T> remove(DataComponentType type);

  <T> T get(DataComponentType type);

  <T> T getOrDefault(DataComponentType type, T defaultValue);

  Map<DataComponentType, @Nullable ItemComponent<?>> getComponents();

  ItemComponentMap read(Object buf, ProtocolVersion version);

  @Deprecated(forRemoval = true)
  default void write(ProtocolVersion version, Object buf) {
    this.write(buf, version);
  }

  ItemComponentMap write(Object buf, ProtocolVersion version);
}
