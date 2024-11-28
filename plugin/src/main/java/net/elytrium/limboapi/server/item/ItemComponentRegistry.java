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

import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.protocol.item.DataComponentType;
import net.elytrium.limboapi.server.item.type.AdventureModePredicateComponent;
import net.elytrium.limboapi.server.item.type.ArmorTrimComponent;
import net.elytrium.limboapi.server.item.type.AttributeModifiersComponent;
import net.elytrium.limboapi.server.item.type.BannerPatternLayersComponent;
import net.elytrium.limboapi.server.item.type.BeesComponent;
import net.elytrium.limboapi.server.item.type.BlockStatePropertiesComponent;
import net.elytrium.limboapi.server.item.type.BooleanComponent;
import net.elytrium.limboapi.server.item.type.ConsumableComponent;
import net.elytrium.limboapi.server.item.type.DeathProtectionComponent;
import net.elytrium.limboapi.server.item.type.EquippableComponent;
import net.elytrium.limboapi.server.item.type.FireworkExplosionComponent;
import net.elytrium.limboapi.server.item.type.FireworksComponent;
import net.elytrium.limboapi.server.item.type.InstrumentComponent;
import net.elytrium.limboapi.server.item.type.ItemStackComponent;
import net.elytrium.limboapi.server.item.type.JukeboxPlayableComponent;
import net.elytrium.limboapi.server.item.type.LodestoneTrackerComponent;
import net.elytrium.limboapi.server.item.type.VarIntArrayComponent;
import net.elytrium.limboapi.server.item.type.HolderSetComponent;
import net.elytrium.limboapi.server.item.type.TextComponent;
import net.elytrium.limboapi.server.item.type.TextListComponent;
import net.elytrium.limboapi.server.item.type.DyedColorComponent;
import net.elytrium.limboapi.server.item.type.PotionContentsComponent;
import net.elytrium.limboapi.server.item.type.SuspiciousStewEffectsComponent;
import net.elytrium.limboapi.server.item.type.UnitComponent;
import net.elytrium.limboapi.server.item.type.EnchantmentsComponent;
import net.elytrium.limboapi.server.item.type.FoodPropertiesComponent;
import net.elytrium.limboapi.server.item.type.ResolvableProfileComponent;
import net.elytrium.limboapi.server.item.type.IntComponent;
import net.elytrium.limboapi.server.item.type.ItemStackListComponent;
import net.elytrium.limboapi.server.item.type.StringComponent;
import net.elytrium.limboapi.server.item.type.BinaryTagComponent;
import net.elytrium.limboapi.server.item.type.ToolComponent;
import net.elytrium.limboapi.server.item.type.UseCooldownComponent;
import net.elytrium.limboapi.server.item.type.VarIntComponent;
import net.elytrium.limboapi.server.item.type.WritableBookContentComponent;
import net.elytrium.limboapi.server.item.type.WrittenBookContentComponent;
import net.elytrium.limboapi.utils.JsonParser;

@SuppressWarnings({"deprecation", "unchecked"})
public class ItemComponentRegistry {

  private static final EnumMap<ProtocolVersion, Map.Entry<IntObjectHashMap<DataComponentType>, Object2IntMap<DataComponentType>>> REGISTRY;
  private static final EnumMap<DataComponentType, Supplier<AbstractItemComponent<?>>> FACTORY;

  static {
    LinkedTreeMap<String, LinkedTreeMap<String, Number>> mappings = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/data_component_types_mappings.json"));
    REGISTRY = new EnumMap<>(ProtocolVersion.class);
    FACTORY = new EnumMap<>(DataComponentType.class);
    mappings.forEach((name, versions) -> {
      DataComponentType type = DataComponentType.valueOf(name.substring(10/*"minecraft:".length()*/).toUpperCase(Locale.US));
      versions.forEach((version, id) -> {
        var entry = ItemComponentRegistry.REGISTRY.computeIfAbsent(ProtocolVersion.getProtocolVersion(Integer.parseInt(version)), key -> {
          Object2IntOpenHashMap<DataComponentType> t2i = new Object2IntOpenHashMap<>(2);
          t2i.defaultReturnValue(Integer.MIN_VALUE);
          return Map.entry(new IntObjectHashMap<>(2), t2i);
        });
        int component = id.intValue();
        entry.getKey().put(component, type);
        entry.getValue().put(type, component);
      });
    });

    ItemComponentRegistry.register(BinaryTagComponent::new,
        DataComponentType.ENTITY_DATA, DataComponentType.BUCKET_ENTITY_DATA, DataComponentType.BLOCK_ENTITY_DATA,
        // ˅ I guess they forgot to add networkSynchronized codec for those below ˅
        DataComponentType.CUSTOM_DATA, DataComponentType.INTANGIBLE_PROJECTILE, DataComponentType.MAP_DECORATIONS, DataComponentType.DEBUG_STICK_STATE,
        DataComponentType.RECIPES, DataComponentType.LOCK, DataComponentType.CONTAINER_LOOT
    );
    ItemComponentRegistry.register(VarIntComponent::new,
        DataComponentType.MAX_STACK_SIZE, DataComponentType.MAX_DAMAGE, DataComponentType.DAMAGE, DataComponentType.RARITY, DataComponentType.CUSTOM_MODEL_DATA,
        DataComponentType.REPAIR_COST, DataComponentType.ENCHANTABLE, DataComponentType.MAP_ID, DataComponentType.MAP_POST_PROCESSING, DataComponentType.OMINOUS_BOTTLE_AMPLIFIER,
        DataComponentType.BASE_COLOR
    );
    ItemComponentRegistry.register(BooleanComponent::new, DataComponentType.UNBREAKABLE, DataComponentType.ENCHANTMENT_GLINT_OVERRIDE);
    ItemComponentRegistry.register(TextComponent::new, DataComponentType.CUSTOM_NAME, DataComponentType.ITEM_NAME);
    ItemComponentRegistry.register(() -> new TextListComponent(256), DataComponentType.LORE);
    ItemComponentRegistry.register(EnchantmentsComponent::new, DataComponentType.ENCHANTMENTS, DataComponentType.STORED_ENCHANTMENTS);
    ItemComponentRegistry.register(AdventureModePredicateComponent::new, DataComponentType.CAN_PLACE_ON, DataComponentType.CAN_BREAK);
    ItemComponentRegistry.register(AttributeModifiersComponent::new, DataComponentType.ATTRIBUTE_MODIFIERS);
    ItemComponentRegistry.register(() -> UnitComponent.INSTANCE,
        DataComponentType.HIDE_ADDITIONAL_TOOLTIP, DataComponentType.HIDE_TOOLTIP,
        DataComponentType.CREATIVE_SLOT_LOCK, DataComponentType.FIRE_RESISTANT, DataComponentType.GLIDER
    );
    ItemComponentRegistry.register(FoodPropertiesComponent::new, DataComponentType.FOOD);
    ItemComponentRegistry.register(ConsumableComponent::new, DataComponentType.CONSUMABLE);
    ItemComponentRegistry.register(() -> new ItemStackComponent(false), DataComponentType.USE_REMAINDER);
    ItemComponentRegistry.register(UseCooldownComponent::new, DataComponentType.USE_COOLDOWN);
    ItemComponentRegistry.register(ToolComponent::new, DataComponentType.TOOL);
    ItemComponentRegistry.register(EquippableComponent::new, DataComponentType.EQUIPPABLE);
    ItemComponentRegistry.register(HolderSetComponent::new, DataComponentType.REPAIRABLE);
    ItemComponentRegistry.register(DeathProtectionComponent::new, DataComponentType.DEATH_PROTECTION);
    ItemComponentRegistry.register(DyedColorComponent::new, DataComponentType.DYED_COLOR);
    ItemComponentRegistry.register(IntComponent::new, DataComponentType.MAP_COLOR);
    ItemComponentRegistry.register(() -> new ItemStackListComponent(false), DataComponentType.CHARGED_PROJECTILES, DataComponentType.BUNDLE_CONTENTS);
    ItemComponentRegistry.register(PotionContentsComponent::new, DataComponentType.POTION_CONTENTS);
    ItemComponentRegistry.register(SuspiciousStewEffectsComponent::new, DataComponentType.SUSPICIOUS_STEW_EFFECTS);
    ItemComponentRegistry.register(WritableBookContentComponent::new, DataComponentType.WRITABLE_BOOK_CONTENT);
    ItemComponentRegistry.register(WrittenBookContentComponent::new, DataComponentType.WRITTEN_BOOK_CONTENT);
    ItemComponentRegistry.register(ArmorTrimComponent::new, DataComponentType.TRIM);
    ItemComponentRegistry.register(InstrumentComponent::new, DataComponentType.INSTRUMENT);
    ItemComponentRegistry.register(JukeboxPlayableComponent::new, DataComponentType.JUKEBOX_PLAYABLE);
    ItemComponentRegistry.register(LodestoneTrackerComponent::new, DataComponentType.LODESTONE_TRACKER);
    ItemComponentRegistry.register(FireworkExplosionComponent::new, DataComponentType.FIREWORK_EXPLOSION);
    ItemComponentRegistry.register(FireworksComponent::new, DataComponentType.FIREWORKS);
    ItemComponentRegistry.register(ResolvableProfileComponent::new, DataComponentType.PROFILE);
    ItemComponentRegistry.register(StringComponent::new, DataComponentType.ITEM_MODEL, DataComponentType.DAMAGE_RESISTANT, DataComponentType.TOOLTIP_STYLE, DataComponentType.NOTE_BLOCK_SOUND);
    ItemComponentRegistry.register(BannerPatternLayersComponent::new, DataComponentType.BANNER_PATTERNS);
    ItemComponentRegistry.register(() -> new VarIntArrayComponent(4), DataComponentType.POT_DECORATIONS);
    ItemComponentRegistry.register(() -> new ItemStackListComponent(true, 256), DataComponentType.CONTAINER);
    ItemComponentRegistry.register(BlockStatePropertiesComponent::new, DataComponentType.BLOCK_STATE);
    ItemComponentRegistry.register(BeesComponent::new, DataComponentType.BEES);
  }

  private static void register(Supplier<AbstractItemComponent<?>> factory, DataComponentType... types) {
    for (DataComponentType type : types) {
      ItemComponentRegistry.FACTORY.put(type, factory);
    }
  }

  public static <T> AbstractItemComponent<T> createComponent(DataComponentType type, ByteBuf buf, ProtocolVersion version) {
    //System.out.println("read item component: " + type);
    var constructor = ItemComponentRegistry.FACTORY.get(type);
    if (constructor == null) {
      throw new IllegalStateException("Component not found: " + type);
    }

    AbstractItemComponent<T> object = (AbstractItemComponent<T>) constructor.get();
    object.read(buf, version);
    return object;
  }

  public static <T> AbstractItemComponent<T> createComponent(DataComponentType type, T value) {
    var constructor = ItemComponentRegistry.FACTORY.get(type);
    if (constructor == null) {
      throw new IllegalStateException("Component not found: " + type);
    }

    AbstractItemComponent<T> object = (AbstractItemComponent<T>) constructor.get();
    object.setValue(value);
    return object;
  }

  public static DataComponentType getType(int id, ProtocolVersion version) {
    DataComponentType type = ItemComponentRegistry.REGISTRY.get(version).getKey().get(id);
    if (type == null) {
      throw new IllegalStateException("Component not found: " + id);
    }

    return type;
  }

  public static int getId(DataComponentType name, ProtocolVersion version) {
    return ItemComponentRegistry.REGISTRY.get(version).getValue().getInt(name);
  }
}
