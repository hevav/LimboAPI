/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.UUID;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

/**
 * The value can be one of the following data types (note that some are version-specific):
 * <pre>
 * {@link Byte},
 * {@link Short} (2-byte short <1.9, VarInt >=1.9),
 * {@link Integer} (4-byte int <=1.8, VarInt >1.8),
 * {@link Long} (4-byte int <=1.8, VarInt <=1.19.2, VarLong >1.19.2),
 * {@link Float}, {@link String}, {@link NBTComponent}, {@link Component}, {@link OptionalNBTComponent}, {@link OptionalComponent}, {@link ItemStack},
 * {@link Boolean}, (serialized as byte data type <=1.8, boolean >1.8)
 * {@link Rotations}, {@link BlockPos}, {@link OptionalBlockPos}, {@link Direction}, {@link OptionalUUID}, {@link BlockState}, {@link OptionalBlockState},
 * {@link CompoundBinaryTag} ({@link EndBinaryTag#endBinaryTag()} Should be used instead as null),
 * {@link Particle}, {@link Particles}, {@link VillagerData},
 * {@link OptionalInt} (Also known as OptionalUnsignedInt) (4-byte int <=1.8, VarInt <=1.13.2, OptionalInt >1.13.2),
 * {@link Pose}, {@link CatVariant}, {@link WolfVariant}, {@link FrogVariant}, {@link OptionalGlobalPos}, {@link PaintingVariant},
 * {@link SnifferState}, {@link ArmadilloState}, {@link Vector3}, {@link Quaternion}.
 * </pre>
 *
 * @see LimboPlayer#setEntityData(int, Collection)
 */
public record EntityDataValue<T>(int id, @Nullable T value) {

  //public EntityDataValue {
  //  System.out.println(value);
  //}

  public record OptionalComponent(@Nullable Component component) {

    public static final OptionalComponent EMPTY = new OptionalComponent(null);
  }

  /**
   * Modern {@link Component} represented in compound nbt tag, used in >=1.20.3. Will be automatically converted to a JSON component when encoding for <1.20.3
   */
  public record NBTComponent(BinaryTag component) {

  }

  public record OptionalNBTComponent(@Nullable NBTComponent component) {

    public static final OptionalNBTComponent EMPTY = new OptionalNBTComponent(null);
  }

  public record OptionalBlockPos(@Nullable BlockPos blockPos) {

    public static final OptionalBlockPos EMPTY = new OptionalBlockPos(null);
  }

  /**
   * @sinceMinecraft 1.8
   */
  public record Rotations(float posX, float posY, float posZ) {

    public Rotations(float posX, float posY, float posZ) {
      this.posX = !Float.isInfinite(posX) && !Float.isNaN(posX) ? posX % 360.0F : 0.0F;
      this.posY = !Float.isInfinite(posY) && !Float.isNaN(posY) ? posY % 360.0F : 0.0F;
      this.posZ = !Float.isInfinite(posZ) && !Float.isNaN(posZ) ? posZ % 360.0F : 0.0F;
    }
  }

  public enum Direction {

    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST;

    public static final Direction[] VALUES = Direction.values();
  }

  public record OptionalUUID(@Nullable UUID uuid) {

    public static final OptionalUUID EMPTY = new OptionalUUID(null);
  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public record BlockState(int blockState/*TODO use existing api*/) implements Particle.ParticleData {

  }

  public record OptionalBlockState(@Nullable BlockState blockState) {

    public static final OptionalBlockState EMPTY = new OptionalBlockState(null);
  }

  /**
   * @sinceMinecraft 1.13
   */
  public record Particle(int type/*TODO enum + registry*/, @Nullable ParticleData data) {

    /**
     * @sinceMinecraft 1.20.5
     * @param color ARGB
     */
    public record ColorParticleData(int color) implements ParticleData {

      /**
       * @param red must be within range [0.0, 1.0]
       * @param green must be within range [0.0, 1.0]
       * @param blue must be within range [0.0, 1.0]
       */
      public ColorParticleData(float red, float green, float blue) {
        this(1.0F, red, green, blue);
      }

      /**
       * @param alpha must be within range [0.0, 1.0]
       * @param red must be within range [0.0, 1.0]
       * @param green must be within range [0.0, 1.0]
       * @param blue must be within range [0.0, 1.0]
       */
      public ColorParticleData(float alpha, float red, float green, float blue) {
        this(ColorParticleData.as8BitChannel(alpha), ColorParticleData.as8BitChannel(red), ColorParticleData.as8BitChannel(green), ColorParticleData.as8BitChannel(blue));
      }

      /**
       * @param red must be within range [0, 255]
       * @param green must be within range [0, 255]
       * @param blue must be within range [0, 255]
       */
      public ColorParticleData(@IntRange(from = 0, to = 255) int red, @IntRange(from = 0, to = 255) int green, @IntRange(from = 0, to = 255) int blue) {
        this(255, red, green, blue);
      }

      /**
       * @param alpha must be within range [0, 255]
       * @param red must be within range [0, 255]
       * @param green must be within range [0, 255]
       * @param blue must be within range [0, 255]
       */
      public ColorParticleData(@IntRange(from = 0, to = 255) int alpha, @IntRange(from = 0, to = 255) int red, @IntRange(from = 0, to = 255) int green, @IntRange(from = 0, to = 255) int blue) {
        this((alpha << 24) | (red << 16) | (green << 8) | blue);
      }

      public float red() {
        return (float) (this.color >> 16 & 0xFF) / 255.0F;
      }

      public float green() {
        return (float) (this.color >> 8 & 0xFF) / 255.0F;
      }

      public float blue() {
        return (float) (this.color & 0xFF) / 255.0F;
      }

      public float alpha() {
        return (float) (this.color >>> 24) / 255.0F;
      }

      private static int as8BitChannel(float f) {
        return ColorParticleData.floor(f * 255.0F);
      }

      private static int floor(float f) {
        int i = (int) f;
        return f < i ? i - 1 : i;
      }
    }

    /**
     * @sinceMinecraft 1.17
     * @param scale Must be within range [0.01, 4.0]
     */
    public record DustColorTransitionParticleData(float fromRed, float fromGreen, float fromBlue, float toRed, float toGreen, float toBlue, float scale) implements ParticleData {

      public DustColorTransitionParticleData(float fromRed, float fromGreen, float fromBlue, float toRed, float toGreen, float toBlue, float scale) {
        this.fromRed = fromRed;
        this.fromGreen = fromGreen;
        this.fromBlue = fromBlue;
        this.toRed = toRed;
        this.toGreen = toGreen;
        this.toBlue = toBlue;
        this.scale = scale < 0.01F ? 0.01F : Math.min(scale, 4.0F);
      }
    }

    /**
     * @sinceMinecraft 1.13
     * @param scale Must be within range [0.01, 4.0]
     */
    public record DustParticleData(float red, float green, float blue, float scale) implements ParticleData {

      /**
       * @param scale Must be within range [0.01, 4.0]
       */
      public DustParticleData(float red, float green, float blue, float scale) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.scale = scale < 0.01F ? 0.01F : Math.min(scale, 4.0F);
      }
    }

    /**
     * @sinceMinecraft 1.19
     */
    public record SculkChargeParticleData(float roll) implements ParticleData {

    }

    /**
     * @sinceMinecraft 1.19
     */
    public record ShriekParticle(int delay) implements ParticleData {

    }

    /**
     * @sinceMinecraft 1.17
     * @param origin Used only in [1.17-1.18.2]
     */
    public record VibrationParticle(@Deprecated @Nullable BlockPos origin, PositionSource destination, int arrivalInTicks) implements ParticleData {

      public record EntityPositionSource(int entityId, float yOffset) implements PositionSource {

      }

      public sealed interface PositionSource permits BlockPos, EntityPositionSource {

      }
    }

    /**
     * @sinceMinecraft 1.21.2
     */
    public record TargetColorParticle(Vector3 target, int color) implements ParticleData {

    }

    public sealed interface ParticleData permits BlockState,
        ColorParticleData, DustColorTransitionParticleData, DustParticleData,
        ItemStack, SculkChargeParticleData, VibrationParticle, TargetColorParticle, ShriekParticle {

    }
  }

  /**
   * @sinceMinecraft 1.20.5
   */
  public static class Particles extends ArrayList<Particle> {

    public Particles() {

    }

    public Particles(int initialCapacity) {
      super(initialCapacity);
    }
  }

  /**
   * @sinceMinecraft 1.14
   * @param level Must be no less than 1
   */
  public record VillagerData(int type, int profession, int level) {

    public VillagerData(int type, int profession, int level) {
      this.type = type;
      this.profession = profession;
      this.level = Math.max(1, level);
    }
  }

  /**
   * @sinceMinecraft 1.14
   */
  public enum Pose { // TODO registry map

    STANDING,
    FALL_FLYING,
    SLEEPING,
    SWIMMING,
    SPIN_ATTACK,
    CROUCHING,
    /**
     * @sinceMinecraft 1.17
     */
    LONG_JUMPING,
    DYING,
    /**
     * @sinceMinecraft 1.19
     */
    CROAKING,
    /**
     * @sinceMinecraft 1.19
     */
    USING_TONGUE,
    /**
     * @sinceMinecraft 1.19.3
     */
    SITTING,
    /**
     * @sinceMinecraft 1.19
     */
    ROARING,
    /**
     * @sinceMinecraft 1.19
     */
    SNIFFING,
    /**
     * @sinceMinecraft 1.19
     */
    EMERGING,
    /**
     * @sinceMinecraft 1.19
     */
    DIGGING,
    /**
     * @sinceMinecraft 1.20
     */
    SLIDING,
    /**
     * @sinceMinecraft 1.20
     */
    SHOOTING,
    /**
     * @sinceMinecraft 1.20
     */
    INHALING;

    public static final Pose[] VALUES = Pose.values();

    public int getProtocolId(ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) { // after 1.19.3 all new poses were added to end
        return this.ordinal();
      } else {
        return switch (this) {
          case STANDING -> 0;
          case FALL_FLYING -> 1;
          case SLEEPING -> 2;
          case SWIMMING -> 3;
          case SPIN_ATTACK -> 4;
          case CROUCHING -> 5;
          default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_17) ? switch (this) {
            // >=1.17
            case LONG_JUMPING -> 6;
            case DYING -> 7;
            default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19) ? switch (this) {
              // >=1.19
              case CROAKING -> 8;
              case USING_TONGUE -> 9;
              default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3) ? switch (this) {
                // >=1.19.3
                case SITTING -> 10;
                case ROARING -> 11;
                case SNIFFING -> 12;
                case EMERGING -> 13;
                case DIGGING -> 14;
                default -> Pose.fail(this);
              } : switch (this) {
                // >=1.19
                case ROARING -> 10;
                case SNIFFING -> 11;
                case EMERGING -> 12;
                case DIGGING -> 13;
                default -> Pose.fail(this);
              };
            } : Pose.fail(this);
          } : this == Pose.DYING ? 6 : Pose.fail(this);
        };
      }
    }

    public static Pose fromProtocolId(int id, ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
        return Pose.VALUES[id];
      } else {
        return switch (id) {
          case 0 -> Pose.STANDING;
          case 1 -> Pose.FALL_FLYING;
          case 2 -> Pose.SLEEPING;
          case 3 -> Pose.SWIMMING;
          case 4 -> Pose.SPIN_ATTACK;
          case 5 -> Pose.CROUCHING;
          default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_17) ? switch (id) {
            // >=1.17
            case 6 -> Pose.LONG_JUMPING;
            case 7 -> Pose.DYING;
            default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19) ? switch (id) {
              // >=1.19
              case 8 -> Pose.CROAKING;
              case 9 -> Pose.USING_TONGUE;
              default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3) ? switch (id) {
                // >=1.19.3
                case 10 -> Pose.SITTING;
                case 11 -> Pose.ROARING;
                case 12 -> Pose.SNIFFING;
                case 13 -> Pose.EMERGING;
                case 14 -> Pose.DIGGING;
                default -> Pose.fail(id);
              } : switch (id) {
                // >=1.19
                case 10 -> Pose.ROARING;
                case 11 -> Pose.SNIFFING;
                case 12 -> Pose.EMERGING;
                case 13 -> Pose.DIGGING;
                default -> Pose.fail(id);
              };
            } : Pose.fail(id);
          } : id == 6 ? Pose.DYING : Pose.fail(id);
        };
      }
    }

    private static <T> T fail(Object value) {
      throw new IllegalStateException("Unexpected value: " + value);
    }
  }

  /**
   * @sinceMinecraft 1.19
   */
  public record CatVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.20.5
   */
  public record WolfVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.19
   */
  public record FrogVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.19
   */
  public record OptionalGlobalPos(@Nullable GlobalPos globalPos) {

    public static final OptionalGlobalPos EMPTY = new OptionalGlobalPos(null);
  }

  /**
   * @sinceMinecraft 1.19
   */
  public record PaintingVariant(int id) {

  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public enum SnifferState {

    IDLING,
    FEELING_HAPPY,
    SCENTING,
    SNIFFING,
    SEARCHING,
    DIGGING,
    RISING;

    public static final SnifferState[] VALUES = SnifferState.values();
  }

  /**
   * @sinceMinecraft 1.20.5
   */
  public enum ArmadilloState { // 🤠

    IDLE,
    ROLLING,
    SCARED,
    UNROLLING;

    public static final ArmadilloState[] VALUES = ArmadilloState.values();
  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public record Vector3(float x, float y, float z) {

  }

  /**
   * @sinceMinecraft 1.19.4
   */
  public record Quaternion(float x, float y, float z, float w) {

  }
}
