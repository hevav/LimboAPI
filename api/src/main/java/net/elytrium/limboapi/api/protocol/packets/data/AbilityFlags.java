/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

/**
 * For PlayerAbilities packet
 */
public class AbilityFlags {

  public static final int INVULNERABLE = 0b0001;
  public static final int FLYING = 0b0010;
  public static final int ALLOW_FLYING = 0b0100;
  public static final int CREATIVE_MODE = 0b1000;
}
