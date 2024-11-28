/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.item;

/**
 * @sinceMinecraft 1.20.5
 */
public enum DataComponentType {

  CUSTOM_DATA,
  MAX_STACK_SIZE,
  MAX_DAMAGE,
  DAMAGE,
  UNBREAKABLE,
  CUSTOM_NAME,
  ITEM_NAME,
  /**
   * @sinceMinecraft 1.21.2
   */
  ITEM_MODEL,
  LORE,
  RARITY,
  ENCHANTMENTS,
  CAN_PLACE_ON,
  CAN_BREAK,
  ATTRIBUTE_MODIFIERS,
  CUSTOM_MODEL_DATA,
  HIDE_ADDITIONAL_TOOLTIP,
  HIDE_TOOLTIP,
  REPAIR_COST,
  CREATIVE_SLOT_LOCK,
  ENCHANTMENT_GLINT_OVERRIDE,
  INTANGIBLE_PROJECTILE,
  FOOD,
  /**
   * @deprecated Removed in 1.21.2
   */
  @Deprecated
  FIRE_RESISTANT,
  /**
   * @sinceMinecraft 1.21.2
   */
  CONSUMABLE,
  /**
   * @sinceMinecraft 1.21.2
   */
  USE_REMAINDER,
  /**
   * @sinceMinecraft 1.21.2
   */
  USE_COOLDOWN,
  /**
   * @sinceMinecraft 1.21.2
   */
  DAMAGE_RESISTANT,
  TOOL,
  /**
   * @sinceMinecraft 1.21.2
   */
  ENCHANTABLE,
  /**
   * @sinceMinecraft 1.21.2
   */
  EQUIPPABLE,
  /**
   * @sinceMinecraft 1.21.2
   */
  REPAIRABLE,
  /**
   * @sinceMinecraft 1.21.2
   */
  GLIDER,
  /**
   * @sinceMinecraft 1.21.2
   */
  TOOLTIP_STYLE,
  /**
   * @sinceMinecraft 1.21.2
   */
  DEATH_PROTECTION,
  STORED_ENCHANTMENTS,
  DYED_COLOR,
  MAP_COLOR,
  MAP_ID,
  MAP_DECORATIONS,
  MAP_POST_PROCESSING,
  CHARGED_PROJECTILES,
  BUNDLE_CONTENTS,
  POTION_CONTENTS,
  SUSPICIOUS_STEW_EFFECTS,
  WRITABLE_BOOK_CONTENT,
  WRITTEN_BOOK_CONTENT,
  TRIM,
  DEBUG_STICK_STATE,
  ENTITY_DATA,
  BUCKET_ENTITY_DATA,
  BLOCK_ENTITY_DATA,
  INSTRUMENT,
  OMINOUS_BOTTLE_AMPLIFIER,
  /**
   * @sinceMinecraft 1.21
   */
  JUKEBOX_PLAYABLE,
  RECIPES,
  LODESTONE_TRACKER,
  FIREWORK_EXPLOSION,
  FIREWORKS,
  PROFILE,
  NOTE_BLOCK_SOUND,
  BANNER_PATTERNS,
  BASE_COLOR,
  POT_DECORATIONS,
  CONTAINER,
  BLOCK_STATE,
  BEES,
  LOCK,
  CONTAINER_LOOT
}
