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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.BlockEntityVersion;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.utils.JsonParser;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class SimpleBlockEntity implements VirtualBlockEntity {

  private static final Map<String, SimpleBlockEntity> MODERN_ID_MAP = new HashMap<>();

  private final Map<BlockEntityVersion, Integer> versionIds = new EnumMap<>(BlockEntityVersion.class);
  private final String modernId;
  private final String legacyId;

  private SimpleBlockEntity(String modernId, String legacyId) {
    this.modernId = modernId;
    this.legacyId = legacyId;
  }

  @Override
  public String getModernId() {
    return this.modernId;
  }

  @Override
  public String getLegacyId() {
    return this.legacyId == null ? this.modernId : this.legacyId;
  }

  @Override
  public int getId(ProtocolVersion version) {
    return this.getId(BlockEntityVersion.from(version));
  }

  @Override
  public int getId(BlockEntityVersion version) {
    return this.versionIds.getOrDefault(version, Integer.MIN_VALUE);
  }

  @Override
  public boolean isSupportedOn(ProtocolVersion version) {
    return this.versionIds.containsKey(BlockEntityVersion.from(version));
  }

  @Override
  public boolean isSupportedOn(BlockEntityVersion version) {
    return this.versionIds.containsKey(version);
  }

  @Override
  public VirtualBlockEntity.Entry createEntry(VirtualChunk chunk, int posX, int posY, int posZ, CompoundBinaryTag nbt) {
    return new Entry(chunk, posX, posY, posZ, nbt);
  }

  static {
    LinkedTreeMap<String, LinkedTreeMap<String, Number>> blockEntitiesMapping = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/block_entity_types_mappings.json"));
    blockEntitiesMapping.forEach((modernId, versions) -> {
      String v1_9Id = SimpleBlockEntity.modern2v1_9(modernId);
      SimpleBlockEntity simpleBlockEntity = new SimpleBlockEntity(modernId, v1_9Id);
      versions.forEach((version, entity) -> simpleBlockEntity.versionIds.put(BlockEntityVersion.from(ProtocolVersion.getProtocolVersion(Integer.parseInt(version))), entity.intValue()));
      if (v1_9Id != null) {
        simpleBlockEntity.versionIds.put(BlockEntityVersion.MINECRAFT_1_9, SimpleBlockEntity.protocolIdFromV1_9(v1_9Id));
        putLegacy: {
          int legacyId;
          switch (v1_9Id) {
            case "MobSpawner" -> legacyId = 1;
            case "Control" -> legacyId = 2;
            case "Beacon" -> legacyId = 3;
            case "Skull" -> legacyId = 4;
            case "FlowerPot" -> legacyId = 5;
            case "Banner" -> legacyId = 6;
            case "UNKNOWN" -> legacyId = 7;
            case "EndGateway" -> legacyId = 8;
            case "Sign" -> legacyId = 9;
            default -> {
              break putLegacy;
            }
          }

          simpleBlockEntity.versionIds.put(BlockEntityVersion.LEGACY, legacyId);
        }
      }

      SimpleBlockEntity.MODERN_ID_MAP.put(modernId, simpleBlockEntity);
    });

    /*
    try {
      Field field = BlockEntityType.class.getDeclaredField("validBlocks");
      field.setAccessible(true);
      BuiltInRegistries.BLOCK_ENTITY_TYPE.asHolderIdMap().forEach(blockEntityTypeReference -> {
        try {
          String key = blockEntityTypeReference.unwrapKey().orElseThrow().location().toString();
          List<String> validBlocks = ((Set<Block>) field.get(blockEntityTypeReference.value())).stream().map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).filter(block -> !block.equals(key)).toList();
          if (!validBlocks.isEmpty()) {
            System.out.println("\"" + key + "\", \"" + String.join("\", \"", validBlocks) + "\"");
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
    */
    SimpleBlockEntity.registerAliases("minecraft:sign",
        "minecraft:acacia_sign", "minecraft:acacia_wall_sign", "minecraft:bamboo_sign", "minecraft:bamboo_wall_sign", "minecraft:birch_sign", "minecraft:birch_wall_sign",
        "minecraft:cherry_sign", "minecraft:cherry_wall_sign", "minecraft:crimson_sign", "minecraft:crimson_wall_sign", "minecraft:dark_oak_sign", "minecraft:dark_oak_wall_sign",
        "minecraft:jungle_sign", "minecraft:jungle_wall_sign", "minecraft:mangrove_sign", "minecraft:mangrove_wall_sign", "minecraft:oak_sign", "minecraft:oak_wall_sign",
        "minecraft:pale_oak_sign", "minecraft:pale_oak_wall_sign", "minecraft:spruce_sign", "minecraft:spruce_wall_sign", "minecraft:warped_sign", "minecraft:warped_wall_sign"
    );
    SimpleBlockEntity.registerAliases("minecraft:hanging_sign",
        "minecraft:acacia_hanging_sign", "minecraft:acacia_wall_hanging_sign", "minecraft:bamboo_hanging_sign", "minecraft:bamboo_wall_hanging_sign",
        "minecraft:birch_hanging_sign", "minecraft:birch_wall_hanging_sign", "minecraft:cherry_hanging_sign", "minecraft:cherry_wall_hanging_sign",
        "minecraft:crimson_hanging_sign", "minecraft:crimson_wall_hanging_sign", "minecraft:dark_oak_hanging_sign", "minecraft:dark_oak_wall_hanging_sign",
        "minecraft:jungle_hanging_sign", "minecraft:jungle_wall_hanging_sign", "minecraft:mangrove_hanging_sign", "minecraft:mangrove_wall_hanging_sign",
        "minecraft:oak_hanging_sign", "minecraft:oak_wall_hanging_sign", "minecraft:pale_oak_hanging_sign", "minecraft:pale_oak_wall_hanging_sign",
        "minecraft:spruce_hanging_sign", "minecraft:spruce_wall_hanging_sign", "minecraft:warped_hanging_sign", "minecraft:warped_wall_hanging_sign"
    );
    SimpleBlockEntity.registerAliases("minecraft:mob_spawner", "minecraft:spawner");
    SimpleBlockEntity.registerAliases("minecraft:piston", "minecraft:moving_piston");
    SimpleBlockEntity.registerAliases("minecraft:skull",
        "minecraft:creeper_head", "minecraft:creeper_wall_head", "minecraft:dragon_head", "minecraft:dragon_wall_head",
        "minecraft:piglin_head", "minecraft:piglin_wall_head", "minecraft:player_head", "minecraft:player_wall_head", "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull",
        "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull", "minecraft:zombie_head", "minecraft:zombie_wall_head"
    );
    SimpleBlockEntity.registerAliases("minecraft:banner",
        "minecraft:black_banner", "minecraft:black_wall_banner", "minecraft:blue_banner", "minecraft:blue_wall_banner", "minecraft:brown_banner", "minecraft:brown_wall_banner",
        "minecraft:cyan_banner", "minecraft:cyan_wall_banner", "minecraft:gray_banner", "minecraft:gray_wall_banner", "minecraft:green_banner", "minecraft:green_wall_banner",
        "minecraft:light_blue_banner", "minecraft:light_blue_wall_banner", "minecraft:light_gray_banner", "minecraft:light_gray_wall_banner", "minecraft:lime_banner", "minecraft:lime_wall_banner",
        "minecraft:magenta_banner", "minecraft:magenta_wall_banner", "minecraft:orange_banner", "minecraft:orange_wall_banner", "minecraft:pink_banner", "minecraft:pink_wall_banner",
        "minecraft:purple_banner", "minecraft:purple_wall_banner", "minecraft:red_banner", "minecraft:red_wall_banner", "minecraft:white_banner", "minecraft:white_wall_banner",
        "minecraft:yellow_banner", "minecraft:yellow_wall_banner"
    );
    SimpleBlockEntity.registerAliases("minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block");
    SimpleBlockEntity.registerAliases("minecraft:shulker_box",
        "minecraft:black_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:cyan_shulker_box", "minecraft:gray_shulker_box", "minecraft:green_shulker_box",
        "minecraft:light_blue_shulker_box", "minecraft:light_gray_shulker_box", "minecraft:lime_shulker_box", "minecraft:magenta_shulker_box", "minecraft:orange_shulker_box",
        "minecraft:pink_shulker_box", "minecraft:purple_shulker_box", "minecraft:red_shulker_box", "minecraft:white_shulker_box", "minecraft:yellow_shulker_box"
    );
    SimpleBlockEntity.registerAliases("minecraft:bed",
        "minecraft:black_bed", "minecraft:blue_bed", "minecraft:brown_bed", "minecraft:cyan_bed", "minecraft:gray_bed", "minecraft:green_bed", "minecraft:light_blue_bed",
        "minecraft:light_gray_bed", "minecraft:lime_bed", "minecraft:magenta_bed", "minecraft:orange_bed", "minecraft:pink_bed", "minecraft:purple_bed", "minecraft:red_bed", "minecraft:white_bed",
        "minecraft:yellow_bed"
    );
    SimpleBlockEntity.registerAliases("minecraft:campfire", "minecraft:soul_campfire");
    SimpleBlockEntity.registerAliases("minecraft:beehive", "minecraft:bee_nest");
    SimpleBlockEntity.registerAliases("minecraft:brushable_block", "minecraft:suspicious_sand", "minecraft:suspicious_gravel");
  }

  private static void registerAliases(String target, String... aliases) {
    SimpleBlockEntity targetBlockEntity = SimpleBlockEntity.MODERN_ID_MAP.get(target);
    for (String alias : aliases) {
      SimpleBlockEntity.MODERN_ID_MAP.put(alias, targetBlockEntity);
    }
  }

  public static SimpleBlockEntity fromModernId(String modernId) {
    SimpleBlockEntity entity = SimpleBlockEntity.MODERN_ID_MAP.get(modernId);
    if (entity == null) {
      LimboAPI.getLogger().warn("BlockEntity {} is not supported, and will be omitted.", modernId);
      return null;
    }

    return entity;
  }

  public static SimpleBlockEntity fromLegacyId(String legacyId) {
    return SimpleBlockEntity.fromModernId(SimpleBlockEntity.legacy2modern(legacyId));
  }

  // https://github.com/ViaVersion/ViaVersion/blob/2aec3ce6d1b82672905c4b6e6a88bdea3c01f16f/common/src/main/java/com/viaversion/viaversion/protocols/protocol1_11to1_10/rewriter/BlockEntityRewriter.java
  private static String modern2v1_9(String modernId) {
    return switch (modernId) {
      case "minecraft:furnace" -> "Furnace";
      case "minecraft:chest" -> "Chest";
      case "minecraft:ender_chest" -> "EnderChest";
      case "minecraft:jukebox" -> "RecordPlayer";
      case "minecraft:dispenser" -> "Trap";
      case "minecraft:dropper" -> "Dropper";
      case "minecraft:sign" -> "Sign";
      case "minecraft:mob_spawner" -> "MobSpawner";
      case "minecraft:noteblock" -> "Music";
      case "minecraft:piston" -> "Piston";
      case "minecraft:brewing_stand" -> "Cauldron";
      case "minecraft:enchanting_table" -> "EnchantTable";
      case "minecraft:end_portal" -> "Airportal";
      case "minecraft:beacon" -> "Beacon";
      case "minecraft:skull" -> "Skull";
      case "minecraft:daylight_detector" -> "DLDetector";
      case "minecraft:hopper" -> "Hopper";
      case "minecraft:comparator" -> "Comparator";
      case "minecraft:flower_pot" -> "FlowerPot";
      case "minecraft:banner" -> "Banner";
      case "minecraft:structure_block" -> "Structure";
      case "minecraft:end_gateway" -> "EndGateway";
      case "minecraft:command_block" -> "Control";
      default -> null;
    };
  }

  private static int protocolIdFromV1_9(String legacyId) {
    return switch (legacyId) {
      case "Furnace" -> 0;
      case "Chest" -> 1;
      case "EnderChest" -> 2;
      case "RecordPlayer" -> 3;
      case "Trap" -> 4;
      case "Dropper" -> 5;
      case "Sign" -> 6;
      case "MobSpawner" -> 7;
      case "Music" -> 8;
      case "Piston" -> 9;
      case "Cauldron" -> 10;
      case "EnchantTable" -> 11;
      case "Airportal" -> 12;
      case "Beacon" -> 13;
      case "Skull" -> 14;
      case "DLDetector" -> 15;
      case "Hopper" -> 16;
      case "Comparator" -> 17;
      case "FlowerPot" -> 18;
      case "Banner" -> 19;
      case "Structure" -> 20;
      case "EndGateway" -> 21;
      case "Control" -> 22;
      default -> throw new IllegalStateException("Unexpected value: " + legacyId);
    };
  }

  private static String legacy2modern(String legacyId) {
    return switch (legacyId) {
      case "Furnace" -> "minecraft:furnace";
      case "Chest" -> "minecraft:chest";
      case "EnderChest" -> "minecraft:ender_chest";
      case "RecordPlayer" -> "minecraft:jukebox";
      case "Trap" -> "minecraft:dispenser";
      case "Dropper" -> "minecraft:dropper";
      case "Sign" -> "minecraft:sign";
      case "MobSpawner" -> "minecraft:mob_spawner";
      case "Music" -> "minecraft:noteblock";
      case "Piston" -> "minecraft:piston";
      case "Cauldron" -> "minecraft:brewing_stand";
      case "EnchantTable" -> "minecraft:enchanting_table";
      case "Airportal" -> "minecraft:end_portal";
      case "Beacon" -> "minecraft:beacon";
      case "Skull" -> "minecraft:skull";
      case "DLDetector" -> "minecraft:daylight_detector";
      case "Hopper" -> "minecraft:hopper";
      case "Comparator" -> "minecraft:comparator";
      case "FlowerPot" -> "minecraft:flower_pot";
      case "Banner" -> "minecraft:banner";
      case "Structure" -> "minecraft:structure_block";
      case "EndGateway" -> "minecraft:end_gateway";
      case "Control" -> "minecraft:command_block";
      default -> legacyId;
    };
  }

  public class Entry implements VirtualBlockEntity.Entry {

    private static boolean warned;

    private final VirtualChunk chunk;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final CompoundBinaryTag nbt;

    public Entry(VirtualChunk chunk, int posX, int posY, int posZ, CompoundBinaryTag nbt) {
      this.chunk = chunk;
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.nbt = nbt;
    }

    @Override
    public VirtualBlockEntity getBlockEntity() {
      return SimpleBlockEntity.this;
    }

    @Override
    public int getPosX() {
      return this.posX;
    }

    @Override
    public int getPosY() {
      return this.posY;
    }

    @Override
    public int getPosZ() {
      return this.posZ;
    }

    @Override
    public CompoundBinaryTag getNbt(ProtocolVersion version) { // adventure-nbt is the worst api I ever used
      CompoundBinaryTag.Builder nbt = null;
      // TODO fix banners 1.20.5-1.21.2 by adding patterns to the registry (but i'm not sure is it important thing and does someone will need it)
      if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        String modernId = SimpleBlockEntity.this.getModernId();
        boolean lessThen16 = version.lessThan(ProtocolVersion.MINECRAFT_1_16);
        boolean noGreaterThan12 = lessThen16 && version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2);
        if (!Entry.warned && this.nbt.get("SkullOwner") != null) {
          LimboAPI.getLogger().warn("The world contains legacy block entities, it's recommended to update your schema to the latest version (>=1.20.5).");
          Entry.warned = true;
        }

        // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_20_3to1_20_5/rewriter/BlockItemPacketRewriter1_20_5.java#L190
        {
          BinaryTag profile = this.nbt.get("profile");
          if (profile != null) {
            nbt = CompoundBinaryTag.builder().put(this.nbt);
            nbt.remove("profile");
            if (profile instanceof StringBinaryTag) {
              // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_15_2to1_16/packets/BlockItemPackets1_16.java#L270
              if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
                nbt.put("SkullOwner", profile);
              }
            } else if (profile instanceof CompoundBinaryTag compound) {
              nbt.put(lessThen16 ? "Owner" : "SkullOwner", Entry.updateProfileTag(compound, version));
            }

            // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_12_2to1_13/block_entity_handlers/SkullHandler.java#L28
            if (noGreaterThan12 && this.chunk != null) {
              int diff = this.getBlock().blockStateId(ProtocolVersion.MINECRAFT_1_13) - 5447/*SKULL_START*/;
              nbt.putByte("SkullType", (byte) Math.floor(diff / 20.0F));
              int pos = diff % 20;
              if (pos >= 4) {
                nbt.putByte("Rot", (byte) ((pos - 4) & 0xFF));
              }
            }
          }
        }

        if (this.nbt.get("patterns") instanceof ListBinaryTag patterns) {
          int i = 0;
          for (BinaryTag pattern : patterns) {
            if (pattern instanceof CompoundBinaryTag patternTag) {
              if (patternTag.get("color") instanceof StringBinaryTag colorTag) {
                String legacy = Entry.patternIdModern2Legacy(patternTag.getString("pattern", ""));
                if (legacy != null) {
                  int color = Entry.colorIdModern2Legacy(colorTag.value());
                  patterns = patterns.set(i, CompoundBinaryTag.builder().put(patternTag)
                      .remove("pattern")
                      .remove("color")
                      .putString("Pattern", legacy)
                      .putInt("Color", noGreaterThan12 ? 15 - color : color)
                      .build(), null
                  );
                }
              }
            }

            ++i;
          }

          if (nbt == null) {
            nbt = CompoundBinaryTag.builder().put(this.nbt);
          }

          nbt.remove("patterns");
          nbt.put("Patterns", patterns);

          if (noGreaterThan12 && this.chunk != null) {
            short id = this.getBlock().blockStateId(ProtocolVersion.MINECRAFT_1_13);
            if (id >= 6854/*BANNER_START*/ && id <= 7109/*BANNER_STOP*/) {
              nbt.putInt("Base", 15 - ((id - 6854/*BANNER_START*/) >> 4));
            } else if (id >= 7110/*WALL_BANNER_START*/ && id <= 7173/*WALL_BANNER_STOP*/) {
              nbt.putInt("Base", 15 - ((id - 7110/*WALL_BANNER_START*/) >> 2));
            } else {
              LimboAPI.getLogger().warn("Why does this block have the banner block entity? nbt={}", this.nbt);
            }
          }
        }

        if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
          // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_20to1_20_2/rewriter/BlockItemPacketRewriter1_20_2.java#L397
          BinaryTag primaryEffect = this.nbt.get("primary_effect");
          BinaryTag secondaryEffect = this.nbt.get("secondary_effect");
          boolean primary = primaryEffect instanceof StringBinaryTag;
          boolean secondary = secondaryEffect instanceof StringBinaryTag;
          if (primary || secondary) {
            if (nbt == null) {
              nbt = CompoundBinaryTag.builder().put(this.nbt);
            }

            if (primary) {
              nbt.remove("primary_effect");
              nbt.putInt("Primary", Entry.potionEffectLegacyId(((StringBinaryTag) primaryEffect).value()) + 1/*Empty effect at 0*/);
            }

            if (secondary) {
              nbt.remove("secondary_effect");
              nbt.putInt("Secondary", Entry.potionEffectLegacyId(((StringBinaryTag) secondaryEffect).value()) + 1/*Empty effect at 0*/);
            }
          }

          if (version.lessThan(ProtocolVersion.MINECRAFT_1_20)) {
            // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_19_4to1_20/packets/BlockItemPackets1_20.java#L191
            {
              int id = SimpleBlockEntity.this.getId(ProtocolVersion.MINECRAFT_1_20);
              if (id == 7 || id == 8) {
                BinaryTag frontText = this.nbt.get("front_text");
                if (frontText != null || this.nbt.get("back_text") != null) {
                  if (nbt == null) {
                    nbt = CompoundBinaryTag.builder().put(this.nbt);
                  }

                  nbt.remove("front_text");
                  nbt.remove("back_text");
                  if (version.greaterThan(ProtocolVersion.MINECRAFT_1_9_2) && frontText instanceof CompoundBinaryTag frontTextTag) {
                    if (frontTextTag.get("messages") instanceof ListBinaryTag messages) {
                      boolean noGreaterThan11 = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_11_1);
                      int i = 0;
                      var serializer = ProtocolUtils.getJsonChatSerializer(version);
                      for (BinaryTag message : messages) {
                        nbt.put("Text" + ++i, noGreaterThan11 ? StringBinaryTag.stringBinaryTag(serializer.serialize(serializer.deserialize(((StringBinaryTag) message).value()))) : message);
                      }
                    }

                    if (frontTextTag.get("filtered_messages") instanceof ListBinaryTag filteredMessages) {
                      int i = 0;
                      for (BinaryTag message : filteredMessages) {
                        nbt.put("FilteredText" + ++i, message);
                      }
                    }

                    BinaryTag color = frontTextTag.get("color");
                    if (color != null) {
                      nbt.put("Color", color);
                    }

                    BinaryTag glowing = frontTextTag.get("has_glowing_text");
                    if (glowing != null) {
                      nbt.put("GlowingText", glowing);
                    }
                  }
                }
              }
            }

            // https://github.com/ViaVersion/ViaBackwards/blob/5.1.1/common/src/main/java/com/viaversion/viabackwards/protocol/v1_18to1_17_1/rewriter/BlockItemPacketRewriter1_18.java#249
            if (version.lessThan(ProtocolVersion.MINECRAFT_1_18)) {
              if (nbt == null) {
                nbt = CompoundBinaryTag.builder().put(this.nbt);
              }

              {
                nbt.put("x", IntBinaryTag.intBinaryTag(this.posX));
                nbt.put("y", IntBinaryTag.intBinaryTag(this.posY));
                nbt.put("z", IntBinaryTag.intBinaryTag(this.posZ));
                String id = version.noLessThan(ProtocolVersion.MINECRAFT_1_11) ? SimpleBlockEntity.this.getModernId() : SimpleBlockEntity.this.getLegacyId();
                BinaryTag currentId = this.nbt.get("id");
                if (currentId == null || !((StringBinaryTag) currentId).value().equals(id)) {
                  nbt.putString("id", id);
                }
              }

              if (SimpleBlockEntity.this.getId(ProtocolVersion.MINECRAFT_1_17_1) == 8 && this.nbt.get("SpawnData") instanceof CompoundBinaryTag spawnData) {
                CompoundBinaryTag entityData = spawnData.getCompound("entity");
                label: if (entityData != CompoundBinaryTag.empty()) {
                  // https://github.com/ViaVersion/ViaBackwards/blob/5.1.1/common/src/main/java/com/viaversion/viabackwards/protocol/v1_13to1_12_2/block_entity_handlers/SpawnerHandler.java
                  if (noGreaterThan12) {
                    String id = entityData.getString("id");
                    String legacyId = switch (id) {
                      case "minecraft:command_block_minecart" -> "minecraft:commandblock_minecart";
                      case "minecraft:end_crystal" -> "minecraft:ender_crystal";
                      case "minecraft:evoker_fangs" -> "minecraft:evocation_fangs";
                      case "minecraft:evoker" -> "minecraft:evocation_illager";
                      case "minecraft:eye_of_ender" -> "minecraft:eye_of_ender_signal";
                      case "minecraft:firework_rocket" -> "minecraft:fireworks_rocket";
                      case "minecraft:illusioner" -> "minecraft:illusion_illager";
                      case "minecraft:snow_golem" -> "minecraft:snowman";
                      case "minecraft:iron_golem" -> "minecraft:villager_golem";
                      case "minecraft:vindicator" -> "minecraft:vindication_illager";
                      case "minecraft:experience_bottle" -> "minecraft:xp_bottle";
                      case "minecraft:experience_orb" -> "minecraft:xp_orb";
                      default -> id;
                    };
                    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_10)) {
                      legacyId = Entry.entityIdModern2Legacy(legacyId);
                    }

                    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
                      nbt.remove("SpawnData");
                      nbt.putString("EntityId", legacyId);
                      break label;
                    }

                    entityData = entityData.putString("id", legacyId);
                  }

                  nbt.put("SpawnData", entityData);
                }
              }

              // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_15_2to1_16/packets/BlockItemPackets1_16.java#L261
              if (lessThen16 && modernId.equals("minecraft:conduit") && this.nbt.get("Target") instanceof IntArrayBinaryTag targetUuid) {
                nbt.remove("Target");
                nbt.put("target_uuid", Entry.uuid(version, targetUuid));
              }

              // https://github.com/ViaVersion/ViaBackwards/blob/5.1.1/common/src/main/java/com/viaversion/viabackwards/protocol/v1_13to1_12_2/block_entity_handlers/BedHandler.java
              if (noGreaterThan12 && version.noLessThan(ProtocolVersion.MINECRAFT_1_12) && this.chunk != null && modernId.equals("minecraft:bed")) {
                nbt.putInt("color", (this.getBlock().blockStateId(ProtocolVersion.MINECRAFT_1_13) - 748) >> 4);
              }

              if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
                String itemId = this.nbt.getString("Item");
                if (!itemId.isEmpty()) {
                  try {
                    nbt.putInt("Item", Item.valueOf(itemId.substring(10).toUpperCase(Locale.ROOT)).getLegacyId());
                  } catch (IllegalArgumentException e) {
                    nbt.putInt("Item", SimpleItem.fromModernId(itemId).itemId(WorldVersion.LEGACY));
                  }
                }
              }
            }
          }
        }
      }

      return nbt == null ? this.nbt : nbt.build();
    }

    private VirtualBlock getBlock() {
      return this.chunk.getBlock(this.posX & 0x0F, this.posY, this.posZ & 0x0F);
    }

    private static CompoundBinaryTag updateProfileTag(CompoundBinaryTag profileTag, ProtocolVersion version) {
      CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
      {
        BinaryTag name = profileTag.get("name");
        if (name instanceof StringBinaryTag) {
          builder.put("Name", name);
        }

        BinaryTag id = profileTag.get("id");
        BinaryTag propertiesListTag = profileTag.get("properties");
        if (propertiesListTag instanceof ListBinaryTag list) {
          CompoundBinaryTag.Builder propertiesTag = CompoundBinaryTag.builder();
          for (BinaryTag propertyTag : list) {
            if (propertyTag instanceof CompoundBinaryTag property) {
              BinaryTag value = property.get("value");
              if (value == null) {
                value = StringBinaryTag.stringBinaryTag("");
              }

              BinaryTag signature = property.get("signature");
              propertiesTag.put(property.getString("name", ""), ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, List.of(CompoundBinaryTag.from(
                  signature instanceof StringBinaryTag ? Map.of("Value", value, "Signature", signature) : Map.of("Value", value)
              ))));
            }
          }
          CompoundBinaryTag properties = propertiesTag.build();
          builder.put("Properties", properties);
          if (id instanceof IntArrayBinaryTag) {
            // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_16_1to1_16_2/packets/BlockItemPackets1_16_2.java#L140
            BinaryTag firstValue;
            builder.put("Id",
                version.lessThan(ProtocolVersion.MINECRAFT_1_16_2)
                && properties.get("textures") instanceof ListBinaryTag textures
                && textures.size() > 0 && textures.get(0) instanceof CompoundBinaryTag firstTexture && (firstValue = firstTexture.get("Value")) != null
                    ? Entry.uuid(version, firstValue.hashCode(), 0, 0, 0)
                    : Entry.uuid(version, (IntArrayBinaryTag) id)
            );
          }
        } else if (id instanceof IntArrayBinaryTag) {
          builder.put("Id", id);
        }
      }

      return builder.build();
    }

    // https://github.com/ViaVersion/ViaBackwards/blob/4.10.2/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_15_2to1_16/packets/BlockItemPackets1_16.java#L268
    private static BinaryTag uuid(ProtocolVersion version, int... array) {
      if (version.lessThan(ProtocolVersion.MINECRAFT_1_16)) {
        return StringBinaryTag.stringBinaryTag(new UUID((long) array[0] << 32 | (array[1] & 0xFFFFFFFFL), (long) array[2] << 32 | (array[3] & 0xFFFFFFFFL)).toString());
      }

      return IntArrayBinaryTag.intArrayBinaryTag(array);
    }

    private static BinaryTag uuid(ProtocolVersion version, IntArrayBinaryTag array) {
      if (version.lessThan(ProtocolVersion.MINECRAFT_1_16)) {
        if (array.size() != 4) {
          return StringBinaryTag.stringBinaryTag("00000000-0000-0000-0000-000000000000");
        }

        return StringBinaryTag.stringBinaryTag(new UUID((long) array.get(0) << 32 | (array.get(1) & 0xFFFFFFFFL), (long) array.get(2) << 32 | (array.get(3) & 0xFFFFFFFFL)).toString());
      }

      return array;
    }

    private static int potionEffectLegacyId(String modern) {
      return switch (modern) {
        case "minecraft:speed" -> 0;
        case "minecraft:slowness" -> 1;
        case "minecraft:haste" -> 2;
        case "minecraft:mining_fatigue" -> 3;
        case "minecraft:strength" -> 4;
        case "minecraft:instant_health" -> 5;
        case "minecraft:instant_damage" -> 6;
        case "minecraft:jump_boost" -> 7;
        case "minecraft:nausea" -> 8;
        case "minecraft:regeneration" -> 9;
        case "minecraft:resistance" -> 10;
        case "minecraft:fire_resistance" -> 11;
        case "minecraft:water_breathing" -> 12;
        case "minecraft:invisibility" -> 13;
        case "minecraft:blindness" -> 14;
        case "minecraft:night_vision" -> 15;
        case "minecraft:hunger" -> 16;
        case "minecraft:weakness" -> 17;
        case "minecraft:poison" -> 18;
        case "minecraft:wither" -> 19;
        case "minecraft:health_boost" -> 20;
        case "minecraft:absorption" -> 21;
        case "minecraft:saturation" -> 22;
        case "minecraft:glowing" -> 23;
        case "minecraft:levitation" -> 24;
        case "minecraft:luck" -> 25;
        case "minecraft:unluck" -> 26;
        case "minecraft:slow_falling" -> 27;
        case "minecraft:conduit_power" -> 28;
        case "minecraft:dolphins_grace" -> 29;
        case "minecraft:bad_omen" -> 30;
        case "minecraft:hero_of_the_village" -> 31;
        case "minecraft:darkness" -> 32;
        default -> -1;
      };
    }

    private static String patternIdModern2Legacy(String modern) {
      return switch (modern) {
        case "minecraft:base" -> "b";
        case "minecraft:square_bottom_left" -> "bl";
        case "minecraft:square_bottom_right" -> "br";
        case "minecraft:square_top_left" -> "tl";
        case "minecraft:square_top_right" -> "tr";
        case "minecraft:stripe_bottom" -> "bs";
        case "minecraft:stripe_top" -> "ts";
        case "minecraft:stripe_left" -> "ls";
        case "minecraft:stripe_right" -> "rs";
        case "minecraft:stripe_center" -> "cs";
        case "minecraft:stripe_middle" -> "ms";
        case "minecraft:stripe_downright" -> "drs";
        case "minecraft:stripe_downleft" -> "dls";
        case "minecraft:small_stripes" -> "ss";
        case "minecraft:cross" -> "cr";
        case "minecraft:straight_cross" -> "sc";
        case "minecraft:triangle_bottom" -> "bt";
        case "minecraft:triangle_top" -> "tt";
        case "minecraft:triangles_bottom" -> "bts";
        case "minecraft:triangles_top" -> "tts";
        case "minecraft:diagonal_left" -> "ld";
        case "minecraft:diagonal_up_right" -> "rd";
        case "minecraft:diagonal_up_left" -> "lud";
        case "minecraft:diagonal_right" -> "rud";
        case "minecraft:circle" -> "mc";
        case "minecraft:rhombus" -> "mr";
        case "minecraft:half_vertical" -> "vh";
        case "minecraft:half_horizontal" -> "hh";
        case "minecraft:half_vertical_right" -> "vhr";
        case "minecraft:half_horizontal_bottom" -> "hhb";
        case "minecraft:border" -> "bo";
        case "minecraft:curly_border" -> "cbo";
        case "minecraft:gradient" -> "gra";
        case "minecraft:gradient_up" -> "gru";
        case "minecraft:bricks" -> "bri";
        case "minecraft:globe" -> "glb";
        case "minecraft:creeper" -> "cre";
        case "minecraft:skull" -> "sku";
        case "minecraft:flower" -> "flo";
        case "minecraft:mojang" -> "moj";
        case "minecraft:piglin" -> "pig";
        default -> null;
      };
    }

    private static int colorIdModern2Legacy(String color) {
      return switch (color) {
        case "orange" -> 1;
        case "magenta" -> 2;
        case "light_blue" -> 3;
        case "yellow" -> 4;
        case "lime" -> 5;
        case "pink" -> 6;
        case "gray" -> 7;
        case "light_gray" -> 8;
        case "cyan" -> 9;
        case "purple" -> 10;
        case "blue" -> 11;
        case "brown" -> 12;
        case "green" -> 13;
        case "red" -> 14;
        case "black" -> 15;
        default -> 0;
      };
    }

    private static String entityIdModern2Legacy(String entityId) {
      return switch (entityId) {
        case "minecraft:area_effect_cloud" -> "AreaEffectCloud";
        case "minecraft:armor_stand" -> "ArmorStand";
        case "minecraft:arrow" -> "Arrow";
        case "minecraft:bat" -> "Bat";
        case "minecraft:blaze" -> "Blaze";
        case "minecraft:boat" -> "Boat";
        case "minecraft:cave_spider" -> "CaveSpider";
        case "minecraft:chicken" -> "Chicken";
        case "minecraft:cow" -> "Cow";
        case "minecraft:creeper" -> "Creeper";
        case "minecraft:donkey" -> "Donkey";
        case "minecraft:dragon_fireball" -> "DragonFireball";
        case "minecraft:elder_guardian" -> "ElderGuardian";
        case "minecraft:ender_crystal" -> "EnderCrystal";
        case "minecraft:ender_dragon" -> "EnderDragon";
        case "minecraft:enderman" -> "Enderman";
        case "minecraft:endermite" -> "Endermite";
        case "minecraft:horse" -> "EntityHorse";
        case "minecraft:eye_of_ender_signal" -> "EyeOfEnderSignal";
        case "minecraft:falling_block" -> "FallingSand";
        case "minecraft:fireball" -> "Fireball";
        case "minecraft:fireworks_rocket" -> "FireworksRocketEntity";
        case "minecraft:ghast" -> "Ghast";
        case "minecraft:giant" -> "Giant";
        case "minecraft:guardian" -> "Guardian";
        case "minecraft:husk" -> "Husk";
        case "minecraft:item" -> "Item";
        case "minecraft:item_frame" -> "ItemFrame";
        case "minecraft:magma_cube" -> "LavaSlime";
        case "minecraft:leash_knot" -> "LeashKnot";
        case "minecraft:chest_minecart" -> "MinecartChest";
        case "minecraft:commandblock_minecart" -> "MinecartCommandBlock";
        case "minecraft:furnace_minecart" -> "MinecartFurnace";
        case "minecraft:hopper_minecart" -> "MinecartHopper";
        case "minecraft:minecart" -> "MinecartRideable";
        case "minecraft:spawner_minecart" -> "MinecartSpawner";
        case "minecraft:tnt_minecart" -> "MinecartTNT";
        case "minecraft:mule" -> "Mule";
        case "minecraft:mooshroom" -> "MushroomCow";
        case "minecraft:ocelot" -> "Ozelot";
        case "minecraft:painting" -> "Painting";
        case "minecraft:pig" -> "Pig";
        case "minecraft:zombie_pigman" -> "PigZombie";
        case "minecraft:polar_bear" -> "PolarBear";
        case "minecraft:tnt" -> "PrimedTnt";
        case "minecraft:rabbit" -> "Rabbit";
        case "minecraft:sheep" -> "Sheep";
        case "minecraft:shulker" -> "Shulker";
        case "minecraft:shulker_bullet" -> "ShulkerBullet";
        case "minecraft:silverfish" -> "Silverfish";
        case "minecraft:skeleton" -> "Skeleton";
        case "minecraft:skeleton_horse" -> "SkeletonHorse";
        case "minecraft:slime" -> "Slime";
        case "minecraft:small_fireball" -> "SmallFireball";
        case "minecraft:snowball" -> "Snowball";
        case "minecraft:snowman" -> "SnowMan";
        case "minecraft:spectral_arrow" -> "SpectralArrow";
        case "minecraft:spider" -> "Spider";
        case "minecraft:squid" -> "Squid";
        case "minecraft:stray" -> "Stray";
        case "minecraft:egg" -> "ThrownEgg";
        case "minecraft:ender_pearl" -> "ThrownEnderpearl";
        case "minecraft:xp_bottle" -> "ThrownExpBottle";
        case "minecraft:potion" -> "ThrownPotion";
        case "minecraft:villager" -> "Villager";
        case "minecraft:villager_golem" -> "VillagerGolem";
        case "minecraft:witch" -> "Witch";
        case "minecraft:wither" -> "WitherBoss";
        case "minecraft:wither_skeleton" -> "WitherSkeleton";
        case "minecraft:wither_skull" -> "WitherSkull";
        case "minecraft:wolf" -> "Wolf";
        case "minecraft:xp_orb" -> "XPOrb";
        case "minecraft:zombie" -> "Zombie";
        case "minecraft:zombie_horse" -> "ZombieHorse";
        case "minecraft:zombie_villager" -> "ZombieVillager";
        default -> entityId;
      };
    }

    @Override
    public String toString() {
      return "Entry{"
             + "chunk=" + this.chunk
             + ", posX=" + this.posX
             + ", posY=" + this.posY
             + ", posZ=" + this.posZ
             + ", nbt=" + this.nbt
             + "}";
    }
  }
}
