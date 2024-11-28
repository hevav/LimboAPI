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

package net.elytrium.limboapi.material;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.EnumMap;
import net.elytrium.limboapi.api.chunk.BuiltInBiome;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag.Builder;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum Biome implements VirtualBiome {

  PLAINS(
      BuiltInBiome.PLAINS,
      "minecraft:plains",
      1,
      new Element(
          true, 0.125F, 0.8F, 0.05F, 0.4F, "plains",
          Element.Effects.builder(0x78A7FF, 0x050533, 0xC0D8FF, 0x3F76E4)
              .moodSound(new Element.Effects.MoodSound(6000, 2.0, 8, "minecraft:ambient.cave"))
              .build()
      )
  ),
  SWAMP(
      BuiltInBiome.SWAMP,
      "minecraft:swamp",
      6,
      new Element(
          true, -0.2F, 0.8F, 0.1F, 0.9F, "swamp",
          Element.Effects.builder(0x78A7FF, 0x232317, 0xC0D8FF, 0x617B64)
              .foliageColor(0x6A7039)
              .grassColorModifier("swamp")
              .moodSound(new Element.Effects.MoodSound(6000, 2.0, 8, "minecraft:ambient.cave"))
              .build()
      )
  ),
  SWAMP_HILLS(
      BuiltInBiome.SWAMP_HILLS,
      "minecraft:swamp_hills",
      134,
      new Element(
          true, -0.1F, 0.8F, 0.3F, 0.9F, "swamp",
          Element.Effects.builder(0x78A7FF, 0x232317, 0xC0D8FF, 0x617B64)
              .foliageColor(0x6A7039)
              .grassColorModifier("swamp")
              .moodSound(new Element.Effects.MoodSound(6000, 2.0, 8, "minecraft:ambient.cave"))
              .build()
      )
  ),
  NETHER_WASTES(
      BuiltInBiome.NETHER_WASTES,
      "minecraft:nether_wastes",
      8,
      new Element(
          false, 0.1F, 2.0F, 0.2F, 0.0F, "nether",
          Element.Effects.builder(0x6EB1FF, 0x050533, 0x330808, 0x3F76E4)
              .moodSound(new Element.Effects.MoodSound(6000, 2.0, 8, "minecraft:ambient.nether_wastes.mood"))
              .build()
      )
  ),
  THE_END(
      BuiltInBiome.THE_END,
      "minecraft:the_end",
      9,
      new Element(
          false, 0.1F, 0.5F, 0.2F, 0.5F, "the_end",
          Element.Effects.builder(0x000000, 0x050533, 0xA080A0, 0x3F76E4)
              .moodSound(new Element.Effects.MoodSound(6000, 2.0, 8, "minecraft:ambient.cave"))
              .build()
      )
  );

  public static final Biome[] VALUES = Biome.values();

  private static final EnumMap<BuiltInBiome, Biome> BUILT_IN_BIOME_MAP = new EnumMap<>(BuiltInBiome.class);

  private final BuiltInBiome index;
  private final String name;
  private final int id;
  private final Element element;

  Biome(BuiltInBiome index, String name, int id, Element element) {
    this.index = index;
    this.name = name;
    this.id = id;
    this.element = element;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public int getId() {
    return this.id;
  }

  public Element getElement() {
    return this.element;
  }

  public CompoundBinaryTag encode(ProtocolVersion version) {
    return CompoundBinaryTag.builder()
        .putString("name", this.name)
        .putInt("id", this.id)
        .put("element", this.element.encode(version))
        .build();
  }

  static {
    for (Biome biome : Biome.VALUES) {
      BUILT_IN_BIOME_MAP.put(biome.index, biome);
    }
  }

  public static Biome of(BuiltInBiome index) {
    return Biome.BUILT_IN_BIOME_MAP.get(index);
  }

  public static CompoundBinaryTag getRegistry(ProtocolVersion version) {
    ListBinaryTag.Builder<BinaryTag> list = ListBinaryTag.builder();
    for (Biome biome : Biome.VALUES) {
      list.add(biome.encode(version));
    }

    return CompoundBinaryTag.builder()
        .putString("type", "minecraft:worldgen/biome")
        .put("value", list.build())
        .build();
  }

  public record Element(boolean hasPrecipitation, float depth, float temperature, float scale, float downfall, String category, Effects effects) {

    public CompoundBinaryTag encode(ProtocolVersion version) {
      Builder tagBuilder = CompoundBinaryTag.builder()
          .putFloat("depth", this.depth)
          .putFloat("temperature", this.temperature)
          .putFloat("scale", this.scale)
          .putFloat("downfall", this.downfall)
          .putString("category", this.category)
          .put("effects", this.effects.encode());

      if (version.lessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
        tagBuilder.putString("precipitation", this.hasPrecipitation ? "rain" : "none");
      } else {
        tagBuilder.putBoolean("has_precipitation", this.hasPrecipitation);
      }

      return tagBuilder.build();
    }

    public record Effects(int skyColor, int waterFogColor, int fogColor, int waterColor, @Nullable Integer foliageColor, @Nullable String grassColorModifier,
        Element.Effects.@Nullable Music music, @Nullable String ambientSound, Element.Effects.@Nullable AdditionsSound additionsSound, Element.Effects.@Nullable MoodSound moodSound,
        Element.Effects.@Nullable Particle particle) {

      public CompoundBinaryTag encode() {
        Builder result = CompoundBinaryTag.builder();

        result.putInt("sky_color", this.skyColor);
        result.putInt("water_fog_color", this.waterColor);
        result.putInt("fog_color", this.fogColor);
        result.putInt("water_color", this.waterColor);

        if (this.foliageColor != null) {
          result.putInt("foliage_color", this.foliageColor);
        }

        if (this.grassColorModifier != null) {
          result.putString("grass_color_modifier", this.grassColorModifier);
        }

        if (this.music != null) {
          result.put("music", this.music.encode());
        }

        if (this.ambientSound != null) {
          result.putString("ambient_sound", this.ambientSound);
        }

        if (this.additionsSound != null) {
          result.put("additions_sound", this.additionsSound.encode());
        }

        if (this.moodSound != null) {
          result.put("mood_sound", this.moodSound.encode());
        }

        if (this.particle != null) {
          result.put("particle", this.particle.encode());
        }

        return result.build();
      }

      public static EffectsBuilder builder(int skyColor, int waterFogColor, int fogColor, int waterColor) {
        return new EffectsBuilder()
            .skyColor(skyColor)
            .waterFogColor(waterFogColor)
            .fogColor(fogColor)
            .waterColor(waterColor);
      }

      public record MoodSound(int tickDelay, double offset, int blockSearchExtent, @NonNull String sound) {

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
              .putInt("tick_delay", this.tickDelay)
              .putDouble("offset", this.offset)
              .putInt("block_search_extent", this.blockSearchExtent)
              .putString("sound", this.sound)
              .build();
        }
      }

      public record Music(boolean replaceCurrentMusic, @NonNull String sound, int maxDelay, int minDelay) {

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
              .putBoolean("replace_current_music", this.replaceCurrentMusic)
              .putString("sound", this.sound)
              .putInt("max_delay", this.maxDelay)
              .putInt("min_delay", this.minDelay)
              .build();
        }
      }

      public record AdditionsSound(@NonNull String sound, double tickChance) {

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
              .putString("sound", this.sound)
              .putDouble("tick_chance", this.tickChance)
              .build();
        }
      }

      public record Particle(float probability, Effects.Particle.@NonNull ParticleOptions options) {

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
              .putFloat("probability", this.probability)
              .put("options", this.options.encode())
              .build();
        }

        public record ParticleOptions(@NonNull String type) { // TODO native particles api + option to add particle data

          public CompoundBinaryTag encode() {
            return CompoundBinaryTag.builder()
                .putString("type", this.type)
                .build();
          }
        }
      }

      public static class EffectsBuilder {

        private int skyColor;
        private int waterFogColor;
        private int fogColor;
        private int waterColor;
        private Integer foliageColor;
        private String grassColorModifier;
        private Music music;
        private String ambientSound;
        private AdditionsSound additionsSound;
        private MoodSound moodSound;
        private Particle particle;

        public EffectsBuilder skyColor(int skyColor) {
          this.skyColor = skyColor;
          return this;
        }

        public EffectsBuilder waterFogColor(int waterFogColor) {
          this.waterFogColor = waterFogColor;
          return this;
        }

        public EffectsBuilder fogColor(int fogColor) {
          this.fogColor = fogColor;
          return this;
        }

        public EffectsBuilder waterColor(int waterColor) {
          this.waterColor = waterColor;
          return this;
        }

        public EffectsBuilder foliageColor(Integer foliageColor) {
          this.foliageColor = foliageColor;
          return this;
        }

        public EffectsBuilder grassColorModifier(String grassColorModifier) {
          this.grassColorModifier = grassColorModifier;
          return this;
        }

        public EffectsBuilder music(Music music) {
          this.music = music;
          return this;
        }

        public EffectsBuilder ambientSound(String ambientSound) {
          this.ambientSound = ambientSound;
          return this;
        }

        public EffectsBuilder additionsSound(AdditionsSound additionsSound) {
          this.additionsSound = additionsSound;
          return this;
        }

        public EffectsBuilder moodSound(MoodSound moodSound) {
          this.moodSound = moodSound;
          return this;
        }

        public EffectsBuilder particle(Particle particle) {
          this.particle = particle;
          return this;
        }

        public Effects build() {
          return new Effects(
              this.skyColor,
              this.waterFogColor,
              this.fogColor,
              this.waterColor,
              this.foliageColor,
              this.grassColorModifier,
              this.music,
              this.ambientSound,
              this.additionsSound,
              this.moodSound,
              this.particle
          );
        }

        @Override
        public String toString() {
          return "Biome.Element.Effects.EffectsBuilder{"
                 + "skyColor=" + this.skyColor
                 + ", waterFogColor=" + this.waterFogColor
                 + ", fogColor=" + this.fogColor
                 + ", waterColor=" + this.waterColor
                 + ", foliageColor=" + this.foliageColor
                 + ", grassColorModifier=" + this.grassColorModifier
                 + ", music=" + this.music
                 + ", ambientSound=" + this.ambientSound
                 + ", additionsSound=" + this.additionsSound
                 + ", moodSound=" + this.moodSound
                 + ", particle=" + this.particle
                 + "}";
        }
      }
    }
  }
}
