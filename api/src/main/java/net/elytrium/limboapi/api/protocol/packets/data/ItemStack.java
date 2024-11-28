/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

import net.elytrium.limboapi.api.protocol.item.DataComponentType;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public record ItemStack(int material/*TODO VirtualItem*/, int amount, int data, @Nullable CompoundBinaryTag nbt, @Nullable ItemComponentMap map) implements EntityDataValue.Particle.ParticleData {

  public static final ItemStack EMPTY = new ItemStack(-1, 0, 0, null, null);

  public ItemStack(int material, int amount, @Nullable CompoundBinaryTag nbt) {
    this(material, amount, nbt == null ? 0 : nbt.getInt("Damage", 0), nbt, null);
  }

  public ItemStack(int material, int amount, @Nullable ItemComponentMap map) {
    this(material, amount, map == null ? 0 : map.getOrDefault(DataComponentType.DAMAGE, 0), map);
  }

  public ItemStack(int material, int amount, int data) {
    this(material, amount, data, null, null);
  }

  public ItemStack(int material, int amount, int data, @Nullable CompoundBinaryTag nbt) {
    this(material, amount, data, nbt, null);
  }

  public ItemStack(int material, int amount, int data, @Nullable ItemComponentMap map) {
    this(material, amount, data, null, map);
  }

  public boolean isEmpty(boolean checkDamage) {
    return this == ItemStack.EMPTY || this.material <= 0 || this.amount <= 0 || (checkDamage && (this.data < -32768 || this.data > 65535));
  }
}
