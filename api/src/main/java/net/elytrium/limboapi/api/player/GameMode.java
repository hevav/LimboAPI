/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.player;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum GameMode {

  SURVIVAL,
  CREATIVE,
  ADVENTURE,
  SPECTATOR;

  /**
   * Cached {@link #values()} array to avoid constant array allocation
   */
  private static final GameMode[] VALUES = GameMode.values();

  /**
   * Get the id of this {@link GameMode}
   *
   * @return The id
   *
   * @see #getById(int)
   */
  public short getId() {
    return (short) this.ordinal();
  }

  @Deprecated(forRemoval = true)
  public short getID() {
    return this.getId();
  }

  /**
   * Get a {@link GameMode} by its' id
   *
   * @param id The id
   *
   * @return The {@link GameMode}, or {@code null} if it does not exist
   *
   * @see #getId()
   */
  @Nullable
  public static GameMode getById(int id) {
    return GameMode.VALUES[id];
  }

  @Nullable
  @Deprecated(forRemoval = true)
  public static GameMode getByID(int id) {
    return GameMode.getById(id);
  }
}
