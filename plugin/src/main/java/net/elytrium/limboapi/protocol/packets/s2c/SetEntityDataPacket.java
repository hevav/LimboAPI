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

package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCounted;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.OptionalInt;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.protocol.packets.data.BlockPos;
import net.elytrium.limboapi.api.protocol.packets.data.EntityDataValue;
import net.elytrium.limboapi.api.protocol.packets.data.GlobalPos;
import net.elytrium.limboapi.api.protocol.packets.data.ItemStack;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.world.SimpleParticlesManager;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * @see SetEntityDataPacket#writeOptionalUUID(ByteBuf, EntityDataValue.OptionalUUID)
 */
public class SetEntityDataPacket implements MinecraftPacket, ReferenceCounted { // TODO check for uuid in nbts

  public static final int EOF_MARKER_LEGACY = Byte.MAX_VALUE;
  public static final int EOF_MARKER = -Byte.MIN_VALUE + Byte.MAX_VALUE;

  private int id;
  private Collection<EntityDataValue<?>> packedItems;

  private ByteBuf buf; // TODO remove after proper tests

  public SetEntityDataPacket(int id, Collection<EntityDataValue<?>> packedItems) { // TODO Short2ObjectArrayMap
    this.id = id;
    this.packedItems = packedItems;
  }

  public SetEntityDataPacket() {

  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    int readerIndex = buf.readerIndex();
    try {
      boolean v1_7_x = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);
      if (v1_7_x || version == ProtocolVersion.MINECRAFT_1_8) {
        this.id = v1_7_x ? buf.readInt() : ProtocolUtils.readVarInt(buf);
        byte index;
        while ((index = buf.readByte()) != SetEntityDataPacket.EOF_MARKER_LEGACY) {
          if (this.packedItems == null) {
            this.packedItems = new ArrayList<>();
          }

          this.packedItems.add(SetEntityDataPacket.read(buf, version, index & 0b00011111, (index & 0b11100000) >> 5));
        }
        //System.out.println(this.packedItems);
      } else {
        this.id = ProtocolUtils.readVarInt(buf);
        short id;
        while ((id = buf.readUnsignedByte()) != SetEntityDataPacket.EOF_MARKER) {
          if (this.packedItems == null) {
            this.packedItems = new ArrayList<>();
          }

          this.packedItems.add(SetEntityDataPacket.read(buf, version, id, ProtocolUtils.readVarInt(buf)));
        }
      }
    } catch (Throwable t) {
      this.buf = buf.retainedDuplicate().readerIndex(readerIndex);
      LimboAPI.getLogger().error("Failed to read SetEntityDataPacket (direction={}, version={}, data=\"{}\")", direction, version, Base64.getEncoder().encodeToString(ByteBufUtil.getBytes(this.buf)), t);
      buf.readerIndex(buf.writerIndex());
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (this.buf != null) {
      buf.writeBytes(this.buf);
      return;
    }

    boolean v1_7_x = protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);
    if (v1_7_x || protocolVersion == ProtocolVersion.MINECRAFT_1_8) {
      if (v1_7_x) {
        buf.writeInt(this.id);
      } else {
        ProtocolUtils.writeVarInt(buf, this.id);
      }
      if (this.packedItems != null) {
        this.packedItems.forEach(dataValue -> {
          Object value = dataValue.value();
          buf.writeByte((SetEntityDataPacket.type(protocolVersion, value) << 5 | dataValue.id() & 0b00011111) & 0xFF);
          SetEntityDataPacket.write(buf, protocolVersion, value);
        });
      }

      buf.writeByte(SetEntityDataPacket.EOF_MARKER_LEGACY);
    } else {
      ProtocolUtils.writeVarInt(buf, this.id);
      if (this.packedItems != null) {
        this.packedItems.forEach(dataValue -> {
          buf.writeByte(dataValue.id());
          Object value = dataValue.value();
          int type = SetEntityDataPacket.type(protocolVersion, value);
          //System.out.println("wrote " + dataValue.id() + " as " + type);
          ProtocolUtils.writeVarInt(buf, type);
          SetEntityDataPacket.write(buf, protocolVersion, value);
        });
      }

      buf.writeByte(SetEntityDataPacket.EOF_MARKER);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    this.retain();
    return false; // forward it to the player
  }

  @Override
  public int refCnt() {
    return this.buf == null ? -1 : this.buf.refCnt();
  }

  @Override
  public ReferenceCounted retain() {
    if (this.buf != null) {
      this.buf.retain();
    }

    return this;
  }

  @Override
  public ReferenceCounted retain(int increment) {
    if (this.buf != null) {
      this.buf.retain(increment);
    }

    return this;
  }

  @Override
  public ReferenceCounted touch() {
    if (this.buf != null) {
      this.buf.touch();
    }

    return this;
  }

  @Override
  public ReferenceCounted touch(Object hint) {
    if (this.buf != null) {
      this.buf.touch(hint);
    }

    return this;
  }

  @Override
  public boolean release() {
    return this.buf != null && this.buf.release();
  }

  @Override
  public boolean release(int decrement) {
    return this.buf != null && this.buf.release(decrement);
  }

  private static EntityDataValue<Object> read(ByteBuf buf, ProtocolVersion version, int id, int type) {
    //System.out.println("read " + id + " as " + type);
    return new EntityDataValue<>(id, version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3) ? switch (type) {
      // >=1.19.3
      case 0 -> buf.readByte();
      case 1 -> ProtocolUtils.readVarInt(buf);
      case 2 -> SetEntityDataPacket.readVarLong(buf);
      case 3 -> buf.readFloat();
      case 4 -> ProtocolUtils.readString(buf, Short.MAX_VALUE);
      case 5 -> version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3) ? SetEntityDataPacket.readNBTComponent(buf, version) : SetEntityDataPacket.readComponent(buf, version);
      case 6 -> version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3) ? SetEntityDataPacket.readOptionalNBTComponent(buf, version) : SetEntityDataPacket.readOptionalComponent(buf, version);
      case 7 -> LimboProtocolUtils.readItemStack(buf, version);
      case 8 -> buf.readBoolean();
      case 9 -> SetEntityDataPacket.readRotations(buf);
      case 10 -> LimboProtocolUtils.readBlockPos(buf, version);
      case 11 -> SetEntityDataPacket.readOptionalBlockPos(buf, version);
      case 12 -> SetEntityDataPacket.readDirection(buf);
      case 13 -> SetEntityDataPacket.readOptionalUUID(buf);
      default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19_4) ? switch (type) {
        // >=1.19.4
        case 14 -> SetEntityDataPacket.readBlockState(buf);
        case 15 -> SetEntityDataPacket.readOptionalBlockState(buf);
        case 16 -> LimboProtocolUtils.readCompoundTag(buf, version);
        case 17 -> SetEntityDataPacket.readParticle(buf, version);
        default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5) ? switch (type) {
          // >=1.20.5
          case 18 -> SetEntityDataPacket.readParticles(buf, version);
          case 19 -> SetEntityDataPacket.readVillagerData(buf);
          case 20 -> SetEntityDataPacket.readOptionalUnsignedInt(buf);
          case 21 -> SetEntityDataPacket.readPose(buf, version);
          case 22 -> SetEntityDataPacket.readCatVariant(buf);
          case 23 -> SetEntityDataPacket.readWolfVariant(buf);
          case 24 -> SetEntityDataPacket.readFrogVariant(buf);
          case 25 -> SetEntityDataPacket.readOptionalGlobalPos(buf, version);
          case 26 -> SetEntityDataPacket.readPaintingVariant(buf);
          case 27 -> SetEntityDataPacket.readSnifferState(buf);
          case 28 -> SetEntityDataPacket.readArmadilloState(buf);
          case 29 -> SetEntityDataPacket.readVector3(buf);
          case 30 -> SetEntityDataPacket.readQuaternion(buf);
          default -> SetEntityDataPacket.fail(type);
        } : switch (type) {
          // >=1.19.4
          case 18 -> SetEntityDataPacket.readVillagerData(buf);
          case 19 -> SetEntityDataPacket.readOptionalUnsignedInt(buf);
          case 20 -> SetEntityDataPacket.readPose(buf, version);
          case 21 -> SetEntityDataPacket.readCatVariant(buf);
          case 22 -> SetEntityDataPacket.readFrogVariant(buf);
          case 23 -> SetEntityDataPacket.readOptionalGlobalPos(buf, version);
          case 24 -> SetEntityDataPacket.readPaintingVariant(buf);
          case 25 -> SetEntityDataPacket.readSnifferState(buf);
          case 26 -> SetEntityDataPacket.readVector3(buf);
          case 27 -> SetEntityDataPacket.readQuaternion(buf);
          default -> SetEntityDataPacket.fail(type);
        };
      } : switch (type) {
        // >=1.19.3
        case 14 -> SetEntityDataPacket.readOptionalBlockState(buf);
        case 15 -> LimboProtocolUtils.readCompoundTag(buf, version);
        case 16 -> SetEntityDataPacket.readParticle(buf, version);
        case 17 -> SetEntityDataPacket.readVillagerData(buf);
        case 18 -> SetEntityDataPacket.readOptionalUnsignedInt(buf);
        case 19 -> SetEntityDataPacket.readPose(buf, version);
        case 20 -> SetEntityDataPacket.readCatVariant(buf);
        case 21 -> SetEntityDataPacket.readFrogVariant(buf);
        case 22 -> SetEntityDataPacket.readOptionalGlobalPos(buf, version);
        case 23 -> SetEntityDataPacket.readPaintingVariant(buf);
        default -> SetEntityDataPacket.fail(type);
      };
    } : version.noLessThan(ProtocolVersion.MINECRAFT_1_9) ? switch (type) {
      case 0 -> buf.readByte();
      case 1 -> ProtocolUtils.readVarInt(buf);
      case 2 -> buf.readFloat();
      case 3 -> ProtocolUtils.readString(buf, Short.MAX_VALUE);
      case 4 -> SetEntityDataPacket.readComponent(buf, version);
      default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_13) ? switch (type) {
        // >=1.13
        case 5 -> SetEntityDataPacket.readOptionalComponent(buf, version);
        case 6 -> LimboProtocolUtils.readItemStack(buf, version);
        case 7 -> buf.readBoolean();
        case 8 -> SetEntityDataPacket.readRotations(buf);
        case 9 -> LimboProtocolUtils.readBlockPos(buf, version);
        case 10 -> SetEntityDataPacket.readOptionalBlockPos(buf, version);
        case 11 -> SetEntityDataPacket.readDirection(buf);
        case 12 -> SetEntityDataPacket.readOptionalUUID(buf);
        case 13 -> SetEntityDataPacket.readOptionalBlockState(buf);
        case 14 -> LimboProtocolUtils.readCompoundTag(buf, version);
        case 15 -> SetEntityDataPacket.readParticle(buf, version);
        default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_14) ? switch (type) {
          // >=1.14
          case 16 -> SetEntityDataPacket.readVillagerData(buf);
          case 17 -> SetEntityDataPacket.readOptionalUnsignedInt(buf);
          case 18 -> SetEntityDataPacket.readPose(buf, version);
          default -> version.noLessThan(ProtocolVersion.MINECRAFT_1_19) ? switch (type) {
            // >=1.19
            case 19 -> SetEntityDataPacket.readCatVariant(buf);
            case 20 -> SetEntityDataPacket.readFrogVariant(buf);
            case 21 -> SetEntityDataPacket.readOptionalGlobalPos(buf, version);
            case 22 -> SetEntityDataPacket.readPaintingVariant(buf);
            default -> SetEntityDataPacket.fail(type);
          } : SetEntityDataPacket.fail(type);
        } : SetEntityDataPacket.fail(type);
      } : switch (type) {
        // >=1.9
        case 5 -> LimboProtocolUtils.readItemStack(buf, version);
        case 6 -> buf.readBoolean();
        case 7 -> SetEntityDataPacket.readRotations(buf);
        case 8 -> LimboProtocolUtils.readBlockPos(buf, version);
        case 9 -> SetEntityDataPacket.readOptionalBlockPos(buf, version);
        case 10 -> SetEntityDataPacket.readDirection(buf);
        case 11 -> SetEntityDataPacket.readOptionalUUID(buf);
        case 12 -> SetEntityDataPacket.readOptionalBlockState(buf);
        default -> type == 13 && version.noLessThan(ProtocolVersion.MINECRAFT_1_12) ? LimboProtocolUtils.readCompoundTag(buf, version) : SetEntityDataPacket.fail(type);
      };
    } : switch (type) {
      // [1.7.2 - 1.8.9]
      case 0 -> buf.readByte();
      case 1 -> buf.readShort();
      case 2 -> buf.readInt();
      case 3 -> buf.readFloat();
      case 4 -> ProtocolUtils.readString(buf, Short.MAX_VALUE);
      case 5 -> LimboProtocolUtils.readItemStack(buf, version);
      case 6 -> LimboProtocolUtils.readBlockPos(buf, version);
      default -> type == 7 && version == ProtocolVersion.MINECRAFT_1_8 ? SetEntityDataPacket.readRotations(buf) : SetEntityDataPacket.fail(type);
    });
  }

  private static <T> int type(ProtocolVersion version, T value) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      // TODO switch when update to java 21
      return value instanceof Byte ? 0
          : value instanceof Short || value instanceof Integer ? 1
          : value instanceof Long ? 2
          : value instanceof Float ? 3
          : value instanceof String ? 4
          : value instanceof Component || value instanceof EntityDataValue.NBTComponent ? 5
          : value instanceof EntityDataValue.OptionalComponent || value instanceof EntityDataValue.OptionalNBTComponent ? 6
          : value instanceof ItemStack ? 7
          : value instanceof Boolean ? 8
          : value instanceof EntityDataValue.Rotations ? 9
          : value instanceof BlockPos ? 10
          : value instanceof EntityDataValue.OptionalBlockPos ? 11
          : value instanceof EntityDataValue.Direction ? 12
          : value instanceof EntityDataValue.OptionalUUID ? 13
          : value instanceof EntityDataValue.BlockState ? 14
          : value instanceof EntityDataValue.OptionalBlockState ? 15
          : (value instanceof CompoundBinaryTag || value instanceof EndBinaryTag) ? 16
          : value instanceof EntityDataValue.Particle ? 17
          : value instanceof EntityDataValue.Particles ? 18
          : value instanceof EntityDataValue.VillagerData ? 19
          : value instanceof OptionalInt ? 20
          : value instanceof EntityDataValue.Pose ? 21
          : value instanceof EntityDataValue.CatVariant ? 22
          : value instanceof EntityDataValue.WolfVariant ? 23
          : value instanceof EntityDataValue.FrogVariant ? 24
          : value instanceof GlobalPos || value instanceof EntityDataValue.OptionalGlobalPos ? 25
          : value instanceof EntityDataValue.PaintingVariant ? 26
          : value instanceof EntityDataValue.SnifferState ? 27
          : value instanceof EntityDataValue.ArmadilloState ? 28
          : value instanceof EntityDataValue.Vector3 ? 29
          : value instanceof EntityDataValue.Quaternion ? 30
          : SetEntityDataPacket.fail(value);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      return value instanceof Byte ? 0
          : value instanceof Short || value instanceof Integer ? 1
          : value instanceof Long ? 2
          : value instanceof Float ? 3
          : value instanceof String ? 4
          : value instanceof Component || value instanceof EntityDataValue.NBTComponent ? 5
          : value instanceof EntityDataValue.OptionalComponent || value instanceof EntityDataValue.OptionalNBTComponent ? 6
          : value instanceof ItemStack ? 7
          : value instanceof Boolean ? 8
          : value instanceof EntityDataValue.Rotations ? 9
          : value instanceof BlockPos ? 10
          : value instanceof EntityDataValue.OptionalBlockPos ? 11
          : value instanceof EntityDataValue.Direction ? 12
          : value instanceof EntityDataValue.OptionalUUID ? 13
          : value instanceof EntityDataValue.BlockState ? 14
          : value instanceof EntityDataValue.OptionalBlockState ? 15
          : (value instanceof CompoundBinaryTag || value instanceof EndBinaryTag) ? 16
          : value instanceof EntityDataValue.Particle ? 17
          : value instanceof EntityDataValue.VillagerData ? 18
          : value instanceof OptionalInt ? 19
          : value instanceof EntityDataValue.Pose ? 20
          : value instanceof EntityDataValue.CatVariant ? 21
          : value instanceof EntityDataValue.FrogVariant ? 22
          : value instanceof GlobalPos || value instanceof EntityDataValue.OptionalGlobalPos ? 23
          : value instanceof EntityDataValue.PaintingVariant ? 24
          : value instanceof EntityDataValue.SnifferState ? 25
          : value instanceof EntityDataValue.Vector3 ? 26
          : value instanceof EntityDataValue.Quaternion ? 27
          : SetEntityDataPacket.fail(value);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      return value instanceof Byte ? 0
          : value instanceof Short || value instanceof Integer ? 1
          : value instanceof Long ? 2
          : value instanceof Float ? 3
          : value instanceof String ? 4
          : value instanceof Component || value instanceof EntityDataValue.NBTComponent ? 5
          : value instanceof EntityDataValue.OptionalComponent || value instanceof EntityDataValue.OptionalNBTComponent ? 6
          : value instanceof ItemStack ? 7
          : value instanceof Boolean ? 8
          : value instanceof EntityDataValue.Rotations ? 9
          : value instanceof BlockPos ? 10
          : value instanceof EntityDataValue.OptionalBlockPos ? 11
          : value instanceof EntityDataValue.Direction ? 12
          : value instanceof EntityDataValue.OptionalUUID ? 13
          : (value instanceof EntityDataValue.BlockState || value instanceof EntityDataValue.OptionalBlockState) ? 14
          : (value instanceof CompoundBinaryTag || value instanceof EndBinaryTag) ? 15
          : value instanceof EntityDataValue.Particle ? 16
          : value instanceof EntityDataValue.VillagerData ? 17
          : value instanceof OptionalInt ? 18
          : value instanceof EntityDataValue.Pose ? 19
          : value instanceof EntityDataValue.CatVariant ? 20
          : value instanceof EntityDataValue.FrogVariant ? 21
          : value instanceof GlobalPos || value instanceof EntityDataValue.OptionalGlobalPos ? 22
          : value instanceof EntityDataValue.PaintingVariant ? 23
          : SetEntityDataPacket.fail(value);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        if (value instanceof EntityDataValue.CatVariant) {
          return 19;
        } else if (value instanceof EntityDataValue.FrogVariant) {
          return 20;
        } else if (value instanceof GlobalPos || value instanceof EntityDataValue.OptionalGlobalPos) {
          return 21;
        } else if (value instanceof EntityDataValue.PaintingVariant) {
          return 22;
        }
      }

      return value instanceof Byte ? 0
          : value instanceof Short || value instanceof Integer || value instanceof Long ? 1
          : value instanceof Float ? 2
          : value instanceof String ? 3
          : value instanceof Component || value instanceof EntityDataValue.NBTComponent ? 4
          : value instanceof EntityDataValue.OptionalComponent || value instanceof EntityDataValue.OptionalNBTComponent ? 5
          : value instanceof ItemStack ? 6
          : value instanceof Boolean ? 7
          : value instanceof EntityDataValue.Rotations ? 8
          : value instanceof BlockPos ? 9
          : value instanceof EntityDataValue.OptionalBlockPos ? 10
          : value instanceof EntityDataValue.Direction ? 11
          : value instanceof EntityDataValue.OptionalUUID ? 12
          : (value instanceof EntityDataValue.BlockState || value instanceof EntityDataValue.OptionalBlockState) ? 13
          : (value instanceof CompoundBinaryTag || value instanceof EndBinaryTag) ? 14
          : value instanceof EntityDataValue.Particle ? 15
          : value instanceof EntityDataValue.VillagerData ? 16
          : value instanceof OptionalInt ? 17
          : value instanceof EntityDataValue.Pose ? 18
          : SetEntityDataPacket.fail(value);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
      return value instanceof Byte ? 0
          : value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof OptionalInt ? 1
          : value instanceof Float ? 2
          : value instanceof String ? 3
          : value instanceof Component || value instanceof EntityDataValue.NBTComponent ? 4
          : value instanceof EntityDataValue.OptionalComponent || value instanceof EntityDataValue.OptionalNBTComponent ? 5
          : value instanceof ItemStack ? 6
          : value instanceof Boolean ? 7
          : value instanceof EntityDataValue.Rotations ? 8
          : value instanceof BlockPos ? 9
          : value instanceof EntityDataValue.OptionalBlockPos ? 10
          : value instanceof EntityDataValue.Direction ? 11
          : value instanceof EntityDataValue.OptionalUUID ? 12
          : (value instanceof EntityDataValue.BlockState || value instanceof EntityDataValue.OptionalBlockState) ? 13
          : (value instanceof CompoundBinaryTag || value instanceof EndBinaryTag) ? 14
          : value instanceof EntityDataValue.Particle ? 15
          : SetEntityDataPacket.fail(value);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
      return value instanceof Byte ? 0
          : value instanceof Short || value instanceof Integer || value instanceof Long || (value instanceof OptionalInt optional && optional.isPresent()) ? 1
          : value instanceof Float ? 2
          : value instanceof String ? 3
          : value instanceof Component || (value instanceof EntityDataValue.OptionalComponent optional && optional.component() != null)
            || value instanceof EntityDataValue.NBTComponent || (value instanceof EntityDataValue.OptionalNBTComponent optionalHolder && optionalHolder.component() != null) ? 4
          : value instanceof ItemStack ? 5
          : value instanceof Boolean ? 6
          : value instanceof EntityDataValue.Rotations ? 7
          : value instanceof BlockPos ? 8
          : value instanceof EntityDataValue.OptionalBlockPos ? 9
          : value instanceof EntityDataValue.Direction ? 10
          : value instanceof EntityDataValue.OptionalUUID ? 11
          : (value instanceof EntityDataValue.BlockState || value instanceof EntityDataValue.OptionalBlockState) ? 12
          : (value instanceof CompoundBinaryTag || value instanceof EndBinaryTag) && version.noLessThan(ProtocolVersion.MINECRAFT_1_12) ? 13
          : SetEntityDataPacket.fail(value);
    } else/* if (version.noLessThan(ProtocolVersion.MINECRAFT_1_7_2))*/ {
      return value instanceof Byte || value instanceof Boolean ? 0
          : value instanceof Short ? 1
          : value instanceof Integer || value instanceof Long || (value instanceof OptionalInt optional && optional.isPresent()) ? 2
          : value instanceof Float ? 3
          : value instanceof String
            || value instanceof Component || (value instanceof EntityDataValue.OptionalComponent optional && optional.component() != null)
            || value instanceof EntityDataValue.NBTComponent || (value instanceof EntityDataValue.OptionalNBTComponent optionalHolder && optionalHolder.component() != null) ? 4
          : value instanceof ItemStack ? 5
          : value instanceof BlockPos || (value instanceof EntityDataValue.OptionalBlockPos optional && optional.blockPos() != null) ? 6
          : (value instanceof EntityDataValue.Rotations && version == ProtocolVersion.MINECRAFT_1_8) ? 7
          : SetEntityDataPacket.fail(value);
    }
  }

  private static <T> void write(ByteBuf buf, ProtocolVersion version, T value) {
    if (value instanceof Byte number) {
      buf.writeByte(number);
    } else if (value instanceof Short number) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
        buf.writeShort(number);
      } else {
        ProtocolUtils.writeVarInt(buf, number);
      }
    } else if (value instanceof Integer number) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
        buf.writeInt(number);
      } else {
        ProtocolUtils.writeVarInt(buf, number);
      }
    } else if (value instanceof Long number) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
        SetEntityDataPacket.writeVarLong(buf, number);
      } else {
        ProtocolUtils.writeVarInt(buf, number.intValue());
      }
    } else if (value instanceof Float number) {
      buf.writeFloat(number);
    } else if (value instanceof String string) {
      ProtocolUtils.writeString(buf, string);
    } else if (value instanceof Component component) {
      SetEntityDataPacket.writeComponent(buf, version, component);
    } else if (value instanceof EntityDataValue.NBTComponent nbtComponent) {
      SetEntityDataPacket.writeNBTComponent(buf, version, nbtComponent);
    } else if (value instanceof EntityDataValue.OptionalComponent optional) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
        Component component = optional.component();
        if (component == null) {
          SetEntityDataPacket.fail(value);
        }

        SetEntityDataPacket.writeComponent(buf, version, component);
      } else {
        SetEntityDataPacket.writeOptionalComponent(buf, version, optional);
      }
    } else if (value instanceof EntityDataValue.OptionalNBTComponent optional) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
        EntityDataValue.NBTComponent component = optional.component();
        if (component == null) {
          SetEntityDataPacket.fail(value);
        }

        SetEntityDataPacket.writeNBTComponent(buf, version, component);
      } else {
        SetEntityDataPacket.writeOptionalNBTComponent(buf, version, optional);
      }
    } else if (value instanceof ItemStack itemStack) {
      LimboProtocolUtils.writeItemStack(buf, version, itemStack);
    } else if (value instanceof Boolean bool) {
      buf.writeBoolean(bool);
    } else if (value instanceof EntityDataValue.Rotations rotations) {
      SetEntityDataPacket.writeRotations(buf, rotations);
    } else if (value instanceof BlockPos blockPos) {
      LimboProtocolUtils.writeBlockPos(buf, version, blockPos);
    } else if (value instanceof EntityDataValue.OptionalBlockPos optional) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
        BlockPos blockPos = optional.blockPos();
        if (blockPos == null) {
          SetEntityDataPacket.fail(value);
        }

        LimboProtocolUtils.writeBlockPos(buf, version, blockPos);
      } else {
        SetEntityDataPacket.writeOptionalBlockPos(buf, version, optional);
      }
    } else if (value instanceof EntityDataValue.Direction direction) {
      SetEntityDataPacket.writeDirection(buf, direction);
    } else if (value instanceof EntityDataValue.OptionalUUID optional) {
      SetEntityDataPacket.writeOptionalUUID(buf, optional);
    } else if (value instanceof EntityDataValue.BlockState state) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        // Make it optional
        buf.writeBoolean(true);
      }

      SetEntityDataPacket.writeBlockState(buf, state);
    } else if (value instanceof EntityDataValue.OptionalBlockState optional) {
      SetEntityDataPacket.writeOptionalBlockState(buf, optional);
    } else if (value instanceof BinaryTag binaryTag) {
      LimboProtocolUtils.writeCompoundBinaryTag(buf, version, binaryTag);
    } else if (value instanceof EntityDataValue.Particle particle) {
      SetEntityDataPacket.writeParticle(buf, version, particle);
    } else if (value instanceof EntityDataValue.Particles particles) {
      SetEntityDataPacket.writeParticles(buf, version, particles);
    } else if (value instanceof EntityDataValue.VillagerData villagerData) {
      SetEntityDataPacket.writeVillagerData(buf, villagerData);
    } else if (value instanceof OptionalInt optionalInt) {
      SetEntityDataPacket.writeOptionalUnsignedInt(buf, optionalInt);
    } else if (value instanceof EntityDataValue.Pose pose) {
      SetEntityDataPacket.writePose(buf, version, pose);
    } else if (value instanceof EntityDataValue.CatVariant catVariant) {
      SetEntityDataPacket.writeCatVariant(buf, catVariant);
    } else if (value instanceof EntityDataValue.WolfVariant wolfVariant) {
      SetEntityDataPacket.writeWolfVariant(buf, wolfVariant);
    } else if (value instanceof EntityDataValue.FrogVariant frogVariant) {
      SetEntityDataPacket.writeFrogVariant(buf, frogVariant);
    } else if (value instanceof GlobalPos globalPos) {
      //if (version.noGreaterThan(ProtocolVersion.MINECRAFT_X_X_X)) { // Neither OptionalGlobalPos nor GlobalPos is not used for now
      // Make it optional
      buf.writeBoolean(true);
      //}

      LimboProtocolUtils.writeGlobalPos(buf, version, globalPos);
    } else if (value instanceof EntityDataValue.OptionalGlobalPos optionalGlobalPos) {
      SetEntityDataPacket.writeOptionalGlobalPos(buf, version, optionalGlobalPos);
    } else if (value instanceof EntityDataValue.PaintingVariant paintingVariant) {
      SetEntityDataPacket.writePaintingVariant(buf, paintingVariant);
    } else if (value instanceof EntityDataValue.SnifferState snifferState) {
      SetEntityDataPacket.writeSnifferState(buf, snifferState);
    } else if (value instanceof EntityDataValue.ArmadilloState armadilloState) {
      SetEntityDataPacket.writeArmadilloState(buf, armadilloState);
    } else if (value instanceof EntityDataValue.Vector3 vector3) {
      SetEntityDataPacket.writeVector3(buf, vector3);
    } else if (value instanceof EntityDataValue.Quaternion quaternion) {
      SetEntityDataPacket.writeQuaternion(buf, quaternion);
    } else {
      SetEntityDataPacket.fail(value);
    }
  }

  private static int fail(Object value) {
    throw new DecoderException("Unexpected value: " + value);
  }


  // VarLong - START
  private static Long readVarLong(ByteBuf buf) {
    byte readByte;
    long value = 0;
    int i = 0;
    do {
      readByte = buf.readByte();
      value |= (long) (readByte & 0x7F) << i;
      i += 7;
      if (i > 10 * 7) {
        throw new DecoderException("Varlong too big");
      }
    } while ((readByte & 0x80) == 0x80);
    return value;
  }

  private static void writeVarLong(ByteBuf buf, long value) {
    while ((value & ~0x7F) != 0) {
      buf.writeByte(((int) value & 0x7F) | 0x80);
      value >>>= 7;
    }

    buf.writeByte((int) value);
  }
  // VarLong - START


  // NBTComponent - START
  private static EntityDataValue.NBTComponent readNBTComponent(ByteBuf buf, ProtocolVersion version) {
    return new EntityDataValue.NBTComponent(ProtocolUtils.readBinaryTag(buf, version, BinaryTagIO.reader()));
  }

  private static void writeNBTComponent(ByteBuf buf, ProtocolVersion version, EntityDataValue.NBTComponent value) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      ProtocolUtils.writeBinaryTag(buf, version, value.component());
    } else {
      GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(version);
      ProtocolUtils.writeString(buf, serializer.serialize(serializer.deserializeFromTree(ComponentHolder.deserialize(value.component()))));
    }
  }
  // NBTComponent - END


  // Component - START
  private static Component readComponent(ByteBuf buf, ProtocolVersion version) {
    return ProtocolUtils.getJsonChatSerializer(version).deserialize(ProtocolUtils.readString(buf, 1 << 18));
  }

  private static void writeComponent(ByteBuf buf, ProtocolVersion version, Component value) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      ProtocolUtils.writeBinaryTag(buf, version, ComponentHolder.serialize(GsonComponentSerializer.gson().serializeToTree(value)));
    } else {
      ProtocolUtils.writeString(buf, ProtocolUtils.getJsonChatSerializer(version).serialize(value));
    }
  }
  // Component - END


  // OptionalNBTComponent - START
  private static EntityDataValue.OptionalNBTComponent readOptionalNBTComponent(ByteBuf buf, ProtocolVersion version) {
    return LimboProtocolUtils.readOptional(buf, EntityDataValue.OptionalNBTComponent.EMPTY, () -> new EntityDataValue.OptionalNBTComponent(SetEntityDataPacket.readNBTComponent(buf, version)));
  }

  private static void writeOptionalNBTComponent(ByteBuf buf, ProtocolVersion version, EntityDataValue.OptionalNBTComponent optional) {
    LimboProtocolUtils.writeOptional(buf, optional.component(), value -> SetEntityDataPacket.writeNBTComponent(buf, version, value));
  }
  // OptionalNBTComponent - END


  // OptionalComponent - START
  private static EntityDataValue.OptionalComponent readOptionalComponent(ByteBuf buf, ProtocolVersion version) {
    return LimboProtocolUtils.readOptional(buf, EntityDataValue.OptionalComponent.EMPTY, () -> new EntityDataValue.OptionalComponent(SetEntityDataPacket.readComponent(buf, version)));
  }

  private static void writeOptionalComponent(ByteBuf buf, ProtocolVersion version, EntityDataValue.OptionalComponent optional) {
    LimboProtocolUtils.writeOptional(buf, optional.component(), value -> SetEntityDataPacket.writeComponent(buf, version, value));
  }
  // OptionalComponent - END


  // Rotations - START
  private static EntityDataValue.Rotations readRotations(ByteBuf buf) {
    return new EntityDataValue.Rotations(buf.readFloat(), buf.readFloat(), buf.readFloat());
  }

  private static void writeRotations(ByteBuf buf, EntityDataValue.Rotations value) {
    buf.writeFloat(value.posX());
    buf.writeFloat(value.posY());
    buf.writeFloat(value.posZ());
  }
  // Rotations - END


  // OptionalBlockPos - START
  private static EntityDataValue.OptionalBlockPos readOptionalBlockPos(ByteBuf buf, ProtocolVersion version) {
    return LimboProtocolUtils.readOptional(buf, EntityDataValue.OptionalBlockPos.EMPTY, () -> new EntityDataValue.OptionalBlockPos(LimboProtocolUtils.readBlockPos(buf, version)));
  }

  private static void writeOptionalBlockPos(ByteBuf buf, ProtocolVersion version, EntityDataValue.OptionalBlockPos optional) {
    LimboProtocolUtils.writeOptional(buf, optional.blockPos(), value -> LimboProtocolUtils.writeBlockPos(buf, version, value));
  }
  // OptionalBlockPos - END


  // Direction - START
  private static EntityDataValue.Direction readDirection(ByteBuf buf) {
    return EntityDataValue.Direction.VALUES[ProtocolUtils.readVarInt(buf)];
  }

  private static void writeDirection(ByteBuf buf, EntityDataValue.Direction value) {
    ProtocolUtils.writeVarInt(buf, value.ordinal());
  }
  // Direction - END


  // OptionalUUID - START
  private static EntityDataValue.OptionalUUID readOptionalUUID(ByteBuf buf) {
    return LimboProtocolUtils.readOptional(buf, EntityDataValue.OptionalUUID.EMPTY, () -> new EntityDataValue.OptionalUUID(ProtocolUtils.readUuid(buf)));
  }

  private static void writeOptionalUUID(ByteBuf buf, EntityDataValue.OptionalUUID optional) {
    // See #138 (Server here send uuid that limboauth or another plugin may change)
    LimboProtocolUtils.writeOptional(buf, optional.uuid(), value -> ProtocolUtils.writeUuid(buf, LimboAPI.getClientUniqueId(value)));
  }
  // OptionalUUID - END


  // BlockState - START
  private static EntityDataValue.BlockState readBlockState(ByteBuf buf) {
    return new EntityDataValue.BlockState(ProtocolUtils.readVarInt(buf));
  }

  private static void writeBlockState(ByteBuf buf, EntityDataValue.BlockState value) {
    ProtocolUtils.writeVarInt(buf, value.blockState());
  }
  // BlockState - END


  // OptionalBlockState - START
  private static EntityDataValue.OptionalBlockState readOptionalBlockState(ByteBuf buf) {
    int blockState = ProtocolUtils.readVarInt(buf);
    return blockState == 0 ? EntityDataValue.OptionalBlockState.EMPTY : new EntityDataValue.OptionalBlockState(new EntityDataValue.BlockState(blockState));
  }

  private static void writeOptionalBlockState(ByteBuf buf, EntityDataValue.OptionalBlockState optional) {
    EntityDataValue.BlockState value = optional.blockState();
    if (value == null) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      SetEntityDataPacket.writeBlockState(buf, value);
    }
  }
  // OptionalBlockState - END


  // Particle - START
  private static EntityDataValue.Particle readParticle(ByteBuf buf, ProtocolVersion version) {
    int type = ProtocolUtils.readVarInt(buf);
    return new EntityDataValue.Particle(type, switch (SimpleParticlesManager.fromProtocolId(version, type)) {
      case BLOCK, BLOCK_MARKER, FALLING_DUST, DUST_PILLAR, BLOCK_CRUMBLE -> SetEntityDataPacket.readBlockState(buf);
      case DUST -> SetEntityDataPacket.readDustParticles(buf);
      case DUST_COLOR_TRANSITION -> SetEntityDataPacket.readDustColorTransition(buf, version);
      case ENTITY_EFFECT -> version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5) ? SetEntityDataPacket.readColor(buf) : null;
      case SCULK_CHARGE -> SetEntityDataPacket.readSculkCharge(buf);
      case ITEM -> LimboProtocolUtils.readItemStack(buf, version);
      case VIBRATION -> SetEntityDataPacket.readVibration(buf, version);
      case TRAIL -> SetEntityDataPacket.readTargetColor(buf);
      case SHRIEK -> SetEntityDataPacket.readShriek(buf);
      default -> null;
    });
  }

  private static EntityDataValue.Particle.DustParticleData readDustParticles(ByteBuf buf) {
    return new EntityDataValue.Particle.DustParticleData(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
  }

  private static EntityDataValue.Particle.DustColorTransitionParticleData readDustColorTransition(ByteBuf buf, ProtocolVersion version) {
    float fromRed = buf.readFloat();
    float fromGreen = buf.readFloat();
    float fromBlue = buf.readFloat();
    float toRed;
    float toGreen;
    float toBlue;
    float scale;
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      toRed = buf.readFloat();
      toGreen = buf.readFloat();
      toBlue = buf.readFloat();
      scale = buf.readFloat();
    } else {
      scale = buf.readFloat();
      toRed = buf.readFloat();
      toGreen = buf.readFloat();
      toBlue = buf.readFloat();
    }

    return new EntityDataValue.Particle.DustColorTransitionParticleData(fromRed, fromGreen, fromBlue, toRed, toGreen, toBlue, scale);
  }

  private static EntityDataValue.Particle.ColorParticleData readColor(ByteBuf buf) {
    return new EntityDataValue.Particle.ColorParticleData(buf.readInt());
  }

  private static EntityDataValue.Particle.SculkChargeParticleData readSculkCharge(ByteBuf buf) {
    return new EntityDataValue.Particle.SculkChargeParticleData(buf.readFloat());
  }

  private static EntityDataValue.Particle.VibrationParticle readVibration(ByteBuf buf, ProtocolVersion version) {
    BlockPos origin = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_18_2) ? LimboProtocolUtils.readBlockPos(buf, version) : null;
    EntityDataValue.Particle.VibrationParticle.PositionSource source;
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      int type = ProtocolUtils.readVarInt(buf);
      source = switch (type) {
        case 0 -> LimboProtocolUtils.readBlockPos(buf, version);
        case 1 -> SetEntityDataPacket.readEntityPositionSource(buf);
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };
    } else {
      String type = ProtocolUtils.readString(buf);
      source = switch (type) {
        case "minecraft:block" -> LimboProtocolUtils.readBlockPos(buf, version);
        case "minecraft:entity" -> SetEntityDataPacket.readEntityPositionSource(buf);
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };
    }

    return new EntityDataValue.Particle.VibrationParticle(origin, source, ProtocolUtils.readVarInt(buf));
  }

  private static EntityDataValue.Particle.VibrationParticle.EntityPositionSource readEntityPositionSource(ByteBuf buf) {
    return new EntityDataValue.Particle.VibrationParticle.EntityPositionSource(ProtocolUtils.readVarInt(buf), buf.readFloat());
  }

  private static EntityDataValue.Particle.TargetColorParticle readTargetColor(ByteBuf buf) {
    return new EntityDataValue.Particle.TargetColorParticle(SetEntityDataPacket.readVector3(buf), buf.readInt());
  }

  private static EntityDataValue.Particle.ShriekParticle readShriek(ByteBuf buf) {
    return new EntityDataValue.Particle.ShriekParticle(ProtocolUtils.readVarInt(buf));
  }

  private static void writeParticle(ByteBuf buf, ProtocolVersion version, EntityDataValue.Particle value) {
    ProtocolUtils.writeVarInt(buf, value.type());
    EntityDataValue.Particle.ParticleData data = value.data();
    if (data instanceof EntityDataValue.BlockState state) {
      ProtocolUtils.writeVarInt(buf, state.blockState());
    } else if (data instanceof EntityDataValue.Particle.ColorParticleData color) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        buf.writeInt(color.color());
      } else {
        SetEntityDataPacket.failParticle(version, data);
      }
    } else if (data instanceof EntityDataValue.Particle.DustColorTransitionParticleData transition) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        buf.writeFloat(transition.fromRed());
        buf.writeFloat(transition.fromGreen());
        buf.writeFloat(transition.fromBlue());
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          buf.writeFloat(transition.toRed());
          buf.writeFloat(transition.toGreen());
          buf.writeFloat(transition.toBlue());
          buf.writeFloat(transition.scale());
        } else {
          buf.writeFloat(transition.scale());
          buf.writeFloat(transition.toRed());
          buf.writeFloat(transition.toGreen());
          buf.writeFloat(transition.toBlue());
        }
      } else {
        SetEntityDataPacket.failParticle(version, data);
      }
    } else if (data instanceof EntityDataValue.Particle.DustParticleData dust) {
      buf.writeFloat(dust.red());
      buf.writeFloat(dust.green());
      buf.writeFloat(dust.blue());
      buf.writeFloat(dust.scale());
    } else if (data instanceof ItemStack itemStack) {
      LimboProtocolUtils.writeItemStack(buf, version, itemStack);
    } else if (data instanceof EntityDataValue.Particle.SculkChargeParticleData charge) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        buf.writeFloat(charge.roll());
      } else {
        SetEntityDataPacket.failParticle(version, data);
      }
    } else if (data instanceof EntityDataValue.Particle.VibrationParticle vibration) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_18_2)) {
          BlockPos origin = vibration.origin();
          if (origin == null) {
            throw new NullPointerException("origin");
          }

          LimboProtocolUtils.writeBlockPos(buf, version, origin);
        }

        if (vibration.destination() instanceof BlockPos blockPos) {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
            ProtocolUtils.writeVarInt(buf, 0);
          } else {
            ProtocolUtils.writeString(buf, "minecraft:block");
          }

          LimboProtocolUtils.writeBlockPos(buf, version, blockPos);
        } else if (vibration.destination() instanceof EntityDataValue.Particle.VibrationParticle.EntityPositionSource entity) {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
            ProtocolUtils.writeVarInt(buf, 1);
          } else {
            ProtocolUtils.writeString(buf, "minecraft:entity");
          }

          ProtocolUtils.writeVarInt(buf, entity.entityId());
          buf.writeFloat(entity.yOffset());
        }

        ProtocolUtils.writeVarInt(buf, vibration.arrivalInTicks());
      } else {
        SetEntityDataPacket.failParticle(version, data);
      }
    } else if (data instanceof EntityDataValue.Particle.TargetColorParticle targetColor) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        SetEntityDataPacket.writeVector3(buf, targetColor.target());
        buf.writeInt(targetColor.color());
      } else {
        SetEntityDataPacket.failParticle(version, data);
      }
    } else if (data instanceof EntityDataValue.Particle.ShriekParticle shriek) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        ProtocolUtils.writeVarInt(buf, shriek.delay());
      } else {
        SetEntityDataPacket.failParticle(version, data);
      }
    }
  }

  private static void failParticle(ProtocolVersion version, EntityDataValue.Particle.ParticleData data) {
    throw new DecoderException("Don't know how to encode " + data + " for " + version);
  }
  // Particle - END


  // Particles - START
  private static EntityDataValue.Particles readParticles(ByteBuf buf, ProtocolVersion version) {
    int amount = ProtocolUtils.readVarInt(buf);
    EntityDataValue.Particles particles = new EntityDataValue.Particles(amount);
    for (int i = 0; i < amount; ++i) {
      particles.add(SetEntityDataPacket.readParticle(buf, version));
    }

    return particles;
  }

  private static void writeParticles(ByteBuf buf, ProtocolVersion version, EntityDataValue.Particles values) {
    ProtocolUtils.writeVarInt(buf, values.size());
    values.forEach(value -> SetEntityDataPacket.writeParticle(buf, version, value));
  }
  // Particles - END


  // VillagerData - START
  private static EntityDataValue.VillagerData readVillagerData(ByteBuf buf) {
    return new EntityDataValue.VillagerData(ProtocolUtils.readVarInt(buf), ProtocolUtils.readVarInt(buf), ProtocolUtils.readVarInt(buf));
  }

  private static void writeVillagerData(ByteBuf buf, EntityDataValue.VillagerData value) {
    ProtocolUtils.writeVarInt(buf, value.type());
    ProtocolUtils.writeVarInt(buf, value.profession());
    ProtocolUtils.writeVarInt(buf, value.level());
  }
  // VillagerData - END


  // OptionalUnsignedInt - START
  private static OptionalInt readOptionalUnsignedInt(ByteBuf buf) {
    int result = ProtocolUtils.readVarInt(buf);
    return result == 0 ? OptionalInt.empty() : OptionalInt.of(result - 1);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static void writeOptionalUnsignedInt(ByteBuf buf, OptionalInt value) {
    ProtocolUtils.writeVarInt(buf, value.orElse(-1) + 1);
  }
  // OptionalUnsignedInt - END


  // Pose - START
  private static EntityDataValue.Pose readPose(ByteBuf buf, ProtocolVersion version) {
    return EntityDataValue.Pose.fromProtocolId(ProtocolUtils.readVarInt(buf), version);
  }

  private static void writePose(ByteBuf buf, ProtocolVersion version, EntityDataValue.Pose value) {
    ProtocolUtils.writeVarInt(buf, value.getProtocolId(version));
  }
  // Pose - END


  // CatVariant - START
  private static EntityDataValue.CatVariant readCatVariant(ByteBuf buf) {
    return new EntityDataValue.CatVariant(ProtocolUtils.readVarInt(buf));
  }

  private static void writeCatVariant(ByteBuf buf, EntityDataValue.CatVariant value) {
    ProtocolUtils.writeVarInt(buf, value.id());
  }
  // CatVariant - END


  // WolfVariant - START
  private static EntityDataValue.WolfVariant readWolfVariant(ByteBuf buf) {
    return new EntityDataValue.WolfVariant(ProtocolUtils.readVarInt(buf));
  }

  private static void writeWolfVariant(ByteBuf buf, EntityDataValue.WolfVariant value) {
    ProtocolUtils.writeVarInt(buf, value.id());
  }
  // WolfVariant - END


  // FrogVariant - START
  private static EntityDataValue.FrogVariant readFrogVariant(ByteBuf buf) {
    return new EntityDataValue.FrogVariant(ProtocolUtils.readVarInt(buf));
  }

  private static void writeFrogVariant(ByteBuf buf, EntityDataValue.FrogVariant value) {
    ProtocolUtils.writeVarInt(buf, value.id());
  }
  // FrogVariant - END


  // OptionalGlobalPos - START
  private static EntityDataValue.OptionalGlobalPos readOptionalGlobalPos(ByteBuf buf, ProtocolVersion version) {
    return LimboProtocolUtils.readOptional(buf, EntityDataValue.OptionalGlobalPos.EMPTY, () -> new EntityDataValue.OptionalGlobalPos(LimboProtocolUtils.readGlobalPos(buf, version)));
  }

  private static void writeOptionalGlobalPos(ByteBuf buf, ProtocolVersion version, EntityDataValue.OptionalGlobalPos optional) {
    LimboProtocolUtils.writeOptional(buf, optional.globalPos(), value -> LimboProtocolUtils.writeGlobalPos(buf, version, value));
  }
  // OptionalGlobalPos - END


  // PaintingVariant - START
  private static EntityDataValue.PaintingVariant readPaintingVariant(ByteBuf buf) {
    return new EntityDataValue.PaintingVariant(ProtocolUtils.readVarInt(buf));
  }

  private static void writePaintingVariant(ByteBuf buf, EntityDataValue.PaintingVariant value) {
    ProtocolUtils.writeVarInt(buf, value.id());
  }
  // PaintingVariant - END


  // SnifferState - START
  private static EntityDataValue.SnifferState readSnifferState(ByteBuf buf) {
    return EntityDataValue.SnifferState.VALUES[ProtocolUtils.readVarInt(buf)];
  }

  private static void writeSnifferState(ByteBuf buf, EntityDataValue.SnifferState value) {
    ProtocolUtils.writeVarInt(buf, value.ordinal());
  }
  // SnifferState - END


  // ArmadilloState - START
  private static EntityDataValue.ArmadilloState readArmadilloState(ByteBuf buf) {
    return EntityDataValue.ArmadilloState.VALUES[ProtocolUtils.readVarInt(buf)];
  }

  private static void writeArmadilloState(ByteBuf buf, EntityDataValue.ArmadilloState value) {
    ProtocolUtils.writeVarInt(buf, value.ordinal());
  }
  // ArmadilloState - END


  // Vector3 - START
  private static EntityDataValue.Vector3 readVector3(ByteBuf buf) {
    return new EntityDataValue.Vector3(buf.readFloat(), buf.readFloat(), buf.readFloat());
  }

  private static void writeVector3(ByteBuf buf, EntityDataValue.Vector3 value) {
    buf.writeFloat(value.x());
    buf.writeFloat(value.y());
    buf.writeFloat(value.z());
  }
  // Vector3 - END


  // Quaternion - START
  private static EntityDataValue.Quaternion readQuaternion(ByteBuf buf) {
    return new EntityDataValue.Quaternion(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
  }

  private static void writeQuaternion(ByteBuf buf, EntityDataValue.Quaternion value) {
    buf.writeFloat(value.x());
    buf.writeFloat(value.y());
    buf.writeFloat(value.z());
    buf.writeFloat(value.w());
  }
  // Quaternion - END
}
