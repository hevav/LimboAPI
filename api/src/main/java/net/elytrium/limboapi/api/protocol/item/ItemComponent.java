/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.item;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface ItemComponent<T> {

  void setValue(@Nullable T value);

  @Nullable
  T getValue();
}
