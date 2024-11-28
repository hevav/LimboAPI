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

package net.elytrium.limboapi.protocol.util;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.data.BlockPos;
import net.elytrium.limboapi.api.protocol.packets.data.GlobalPos;
import net.elytrium.limboapi.api.protocol.packets.data.ItemStack;
import net.elytrium.limboapi.server.item.SimpleItemComponentMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LimboProtocolUtils {

  //public static final BinaryTagIO.Reader READER = BinaryTagIO.reader(1 << 21);

  private static final BinaryTagType<? extends BinaryTag>[] BINARY_TAG_TYPES;

  static {
    try {
      Field field = ProtocolUtils.class.getDeclaredField("BINARY_TAG_TYPES");
      field.setAccessible(true);
      BINARY_TAG_TYPES = (BinaryTagType<? extends BinaryTag>[]) field.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T readEither(ByteBuf buf, Supplier<T> left, Supplier<T> right) {
    return buf.readBoolean() ? left.get() : right.get();
  }

  public static <T> T readEither(ByteBuf buf, Function<ByteBuf, T> left, Function<ByteBuf, T> right) {
    return buf.readBoolean() ? left.apply(buf) : right.apply(buf);
  }

  public static <T> T readOptional(ByteBuf buf, Supplier<T> decoder) {
    return buf.readBoolean() ? decoder.get() : null;
  }

  public static <T> T readOptional(ByteBuf buf, T empty, Supplier<T> decoder) {
    return buf.readBoolean() ? decoder.get() : empty;
  }

  public static <T> T readOptional(ByteBuf buf, Function<ByteBuf, T> decoder) {
    return buf.readBoolean() ? decoder.apply(buf) : null;
  }

  public static <T> T readOptional(ByteBuf buf, T empty, Function<ByteBuf, T> decoder) {
    return buf.readBoolean() ? decoder.apply(buf) : empty;
  }

  public static <T> void writeOptional(ByteBuf buf, T value, Consumer<T> encoder) {
    if (value == null) {
      buf.writeBoolean(false);
      return;
    }

    buf.writeBoolean(true);
    encoder.accept(value);
  }

  public static <T> void writeOptional(ByteBuf buf, T value, EncoderByteBufValue<T> encoder) {
    if (value == null) {
      buf.writeBoolean(false);
      return;
    }

    buf.writeBoolean(true);
    encoder.write(buf, value);
  }

  public static <T> void writeOptional(ByteBuf buf, T value, EncoderValueByteBuf<T> encoder) {
    if (value == null) {
      buf.writeBoolean(false);
      return;
    }

    buf.writeBoolean(true);
    encoder.write(value, buf);
  }

  public static <T> Collection<T> readCollection(ByteBuf buf, Supplier<T> decoder) {
    return LimboProtocolUtils.readCollection(buf, Integer.MAX_VALUE, decoder);
  }

  public static <T> Collection<T> readCollection(ByteBuf buf, int limit, Supplier<T> decoder) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      Collection<T> list = new ArrayList<>(amount);
      for (int i = 0; i < amount; ++i) {
        list.add(decoder.get());
      }

      return list;
    } else {
      return List.of();
    }
  }

  public static <T> Collection<T> readCollection(ByteBuf buf, Function<ByteBuf, T> decoder) {
    return LimboProtocolUtils.readCollection(buf, Integer.MAX_VALUE, decoder);
  }

  public static <T> Collection<T> readCollection(ByteBuf buf, int limit, Function<ByteBuf, T> decoder) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      Collection<T> list = new ArrayList<>(amount);
      for (int i = 0; i < amount; ++i) {
        list.add(decoder.apply(buf));
      }

      return list;
    } else {
      return List.of();
    }
  }

  public static <T> void writeCollection(ByteBuf buf, Collection<T> collection, Consumer<T> encoder) {
    LimboProtocolUtils.writeCollection(buf, collection, Integer.MAX_VALUE, encoder);
  }

  public static <T> void writeCollection(ByteBuf buf, Collection<T> collection, int limit, Consumer<T> encoder) {
    if (collection == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = collection.size();
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      collection.forEach(encoder);
    }
  }

  public static <T> void writeCollection(ByteBuf buf, Collection<T> collection, BiConsumer<T, ByteBuf> encoder) {
    LimboProtocolUtils.writeCollection(buf, collection, Integer.MAX_VALUE, encoder);
  }

  public static <T> void writeCollection(ByteBuf buf, Collection<T> collection, int limit, BiConsumer<T, ByteBuf> encoder) {
    if (collection == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = collection.size();
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      collection.forEach(value -> encoder.accept(value, buf));
    }
  }

  public static <T> T @Nullable [] readArray(ByteBuf buf, IntFunction<T[]> generator, Supplier<T> decoder) {
    return LimboProtocolUtils.readArray(buf, Integer.MAX_VALUE, generator, decoder);
  }

  public static <T> T @Nullable [] readArray(ByteBuf buf, int limit, IntFunction<T[]> generator, Supplier<T> decoder) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      T[] array = generator.apply(amount);
      for (int i = 0; i < amount; ++i) {
        array[i] = decoder.get();
      }

      return array;
    } else {
      return generator.apply(0);
    }
  }

  public static <T> void writeArray(ByteBuf buf, T @Nullable [] array, Consumer<T> encoder) {
    LimboProtocolUtils.writeArray(buf, array, Integer.MAX_VALUE, encoder);
  }

  public static <T> void writeArray(ByteBuf buf, T @Nullable [] array, int limit, Consumer<T> encoder) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (T value : array) {
        encoder.accept(value);
      }
    }
  }

  public static long @Nullable [] readLongArray(ByteBuf buf) {
    return LimboProtocolUtils.readLongArray(buf, Integer.MAX_VALUE);
  }

  public static long @Nullable [] readLongArray(ByteBuf buf, int limit) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      long[] array = new long[amount];
      for (int i = 0; i < amount; ++i) {
        array[i] = buf.readLong();
      }

      return array;
    } else {
      return null;
    }
  }

  public static void writeLongArray(ByteBuf buf, long @Nullable [] array) {
    LimboProtocolUtils.writeLongArray(buf, array, Integer.MAX_VALUE);
  }

  public static void writeLongArray(ByteBuf buf, long @Nullable [] array, int limit) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (long value : array) {
        buf.writeLong(value);
      }
    }
  }

  public static int @Nullable [] readIntArray(ByteBuf buf) {
    return LimboProtocolUtils.readIntArray(buf, Integer.MAX_VALUE);
  }

  public static int @Nullable [] readIntArray(ByteBuf buf, int limit) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      int[] array = new int[amount];
      for (int i = 0; i < amount; ++i) {
        array[i] = buf.readInt();
      }

      return array;
    } else {
      return null;
    }
  }

  public static void writeIntArray(ByteBuf buf, int @Nullable [] array) {
    LimboProtocolUtils.writeIntArray(buf, array, Integer.MAX_VALUE);
  }

  public static void writeIntArray(ByteBuf buf, int @Nullable [] array, int limit) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (int value : array) {
        buf.writeInt(value);
      }
    }
  }

  public static int @Nullable [] readVarIntArray(ByteBuf buf) {
    return LimboProtocolUtils.readVarIntArray(buf, Integer.MAX_VALUE);
  }

  public static int @Nullable [] readVarIntArray(ByteBuf buf, int limit) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      int[] array = new int[amount];
      for (int i = 0; i < amount; ++i) {
        array[i] = ProtocolUtils.readVarInt(buf);
      }

      return array;
    } else {
      return null;
    }
  }

  public static void writeVarIntArray(ByteBuf buf, int @Nullable [] array) {
    LimboProtocolUtils.writeVarIntArray(buf, array, Integer.MAX_VALUE);
  }

  public static void writeVarIntArray(ByteBuf buf, int @Nullable [] array, int limit) {
    if (array == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = array.length;
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      for (int value : array) {
        ProtocolUtils.writeVarInt(buf, value);
      }
    }
  }

  public static <K, V> Map<K, V> readMap(ByteBuf buf, Supplier<K> keyDecoder, Supplier<V> valueDecoder) {
    return LimboProtocolUtils.readMap(buf, Integer.MAX_VALUE, keyDecoder, valueDecoder);
  }

  public static <K, V> Map<K, V> readMap(ByteBuf buf, int limit, Supplier<K> keyDecoder, Supplier<V> valueDecoder) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      Map<K, V> map = new HashMap<>(amount);
      for (int i = 0; i < amount; ++i) {
        map.put(keyDecoder.get(), valueDecoder.get());
      }

      return map;
    } else {
      return Map.of();
    }
  }

  public static <K, V> Map<K, V> readMap(ByteBuf buf, Function<ByteBuf, K> keyDecoder, Function<ByteBuf, V> valueDecoder) {
    return LimboProtocolUtils.readMap(buf, Integer.MAX_VALUE, keyDecoder, valueDecoder);
  }

  public static <K, V> Map<K, V> readMap(ByteBuf buf, int limit, Function<ByteBuf, K> keyDecoder, Function<ByteBuf, V> valueDecoder) {
    int amount = ProtocolUtils.readVarInt(buf);
    if (amount > 0) {
      if (amount > limit) {
        throw new DecoderException(amount + " elements exceeded max size of: " + limit);
      }

      Map<K, V> map = new HashMap<>(amount);
      for (int i = 0; i < amount; ++i) {
        map.put(keyDecoder.apply(buf), valueDecoder.apply(buf));
      }

      return map;
    } else {
      return Map.of();
    }
  }

  public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, BiConsumer<K, V> encoder) {
    LimboProtocolUtils.writeMap(buf, map, Integer.MAX_VALUE, encoder);
  }

  public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, int limit, BiConsumer<K, V> encoder) {
    if (map == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = map.size();
    // Inverted to avoid duplicate warning
    if (amount != 0) {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      map.forEach(encoder);
    } else {
      ProtocolUtils.writeVarInt(buf, 0);
    }
  }

  public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, EncoderByteBufValue<K> keyEncoder, EncoderByteBufValue<V> valueEncoder) {
    LimboProtocolUtils.writeMap(buf, map, Integer.MAX_VALUE, keyEncoder, valueEncoder);
  }

  public static <K, V> void writeMap(ByteBuf buf, Map<K, V> map, int limit, EncoderByteBufValue<K> keyEncoder, EncoderByteBufValue<V> valueEncoder) {
    if (map == null) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    int amount = map.size();
    if (amount == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      if (amount > limit) {
        throw new EncoderException(amount + " elements exceeded max size of: " + limit);
      }

      ProtocolUtils.writeVarInt(buf, amount);
      map.forEach((key, value) -> {
        keyEncoder.write(buf, key);
        valueEncoder.write(buf, value);
      });
    }
  }

  public static void writeContainerId(ByteBuf buf, ProtocolVersion version, int id) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      ProtocolUtils.writeVarInt(buf, id);
    } else {
      buf.writeByte(id);
    }
  }

  public static int readContainerId(ByteBuf buf, ProtocolVersion version) {
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? ProtocolUtils.readVarInt(buf) : buf.readUnsignedByte();
  }

  public static GlobalPos readGlobalPos(ByteBuf buf, ProtocolVersion version) {
    return new GlobalPos(ProtocolUtils.readString(buf), LimboProtocolUtils.readBlockPos(buf, version));
  }

  public static void writeGlobalPos(ByteBuf buf, ProtocolVersion version, GlobalPos value) {
    ProtocolUtils.writeString(buf, value.dimension());
    LimboProtocolUtils.writeBlockPos(buf, version, value.blockPos());
  }

  public static BlockPos readBlockPos(ByteBuf buf, ProtocolVersion version) {
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
      return new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    } else {
      long packed = buf.readLong();
      return version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)
          ? new BlockPos((int) (packed >> 38), (int) (packed << 26 >> 52), (int) (packed << 38 >> 38))
          : new BlockPos((int) (packed >> 38), (int) (packed << 52 >> 52), (int) (packed << 26 >> 38));
    }
  }

  public static void writeBlockPos(ByteBuf buf, ProtocolVersion version, BlockPos value) {
    LimboProtocolUtils.writeBlockPos(buf, version, value.posX(), value.posY(), value.posZ());
  }

  public static void writeBlockPos(ByteBuf buf, ProtocolVersion version, int posX, int posY, int posZ) {
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeInt(posX);
      buf.writeInt(posY);
      buf.writeInt(posZ);
    } else {
      buf.writeLong(version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)
          ? ((posX & 0x3FFFFFFL) << 38) | ((posY & 0xFFFL) << 26) | (posZ & 0x3FFFFFFL)
          : ((posX & 0x3FFFFFFL) << 38) | (posY & 0xFFFL) | ((posZ & 0x3FFFFFFL) << 12)
      );
    }
  }

  public static ItemStack readItemStack(ByteBuf buf, ProtocolVersion version) {
    return LimboProtocolUtils.readItemStack(buf, version, true);
  }

  public static ItemStack readItemStack(ByteBuf buf, ProtocolVersion version, boolean allowEmpty) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      int amount = ProtocolUtils.readVarInt(buf);
      return amount > 0
          ? new ItemStack(ProtocolUtils.readVarInt(buf), amount, SimpleItemComponentMap.read(buf, version))
          : LimboProtocolUtils.emptyItemStack(allowEmpty);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
      return buf.readBoolean()
          ? new ItemStack(ProtocolUtils.readVarInt(buf), buf.readByte(), LimboProtocolUtils.readCompoundTagOrNull(buf, version))
          : LimboProtocolUtils.emptyItemStack(allowEmpty);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
      short material = buf.readShort();
      return material >= 0
          ? new ItemStack(material, buf.readByte(), LimboProtocolUtils.readCompoundTagOrNull(buf, version))
          : LimboProtocolUtils.emptyItemStack(allowEmpty);
    } else {
      short material = buf.readShort();
      return material >= 0
          ? new ItemStack(material, buf.readByte(), buf.readShort(), LimboProtocolUtils.readCompoundTagOrNull(buf, version))
          : LimboProtocolUtils.emptyItemStack(allowEmpty);
    }
  }

  private static ItemStack emptyItemStack(boolean allowEmpty) {
    if (allowEmpty) {
      return ItemStack.EMPTY;
    }

    throw new DecoderException("Empty ItemStack not allowed");
  }

  public static void writeItemStack(ByteBuf buf, ProtocolVersion version, ItemStack value) {
    LimboProtocolUtils.writeItemStack(buf, version, value, true);
  }

  public static void writeItemStack(ByteBuf buf, ProtocolVersion version, ItemStack value, boolean allowEmpty) {
    boolean hasDamage = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2);
    if (value.isEmpty(hasDamage)) {
      if (allowEmpty) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          ProtocolUtils.writeVarInt(buf, 0);
        } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
          buf.writeBoolean(false);
        } else {
          buf.writeShort(-1);
        }
      } else {
        throw new EncoderException("Empty ItemStack not allowed");
      }
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtils.writeVarInt(buf, value.amount());
      ProtocolUtils.writeVarInt(buf, value.material());
      ItemComponentMap map = value.map();
      if (map == null) {
        ProtocolUtils.writeVarInt(buf, 0); // added
        ProtocolUtils.writeVarInt(buf, 0); // removed
      } else {
        map.write(buf, version);
      }
    } else {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
        buf.writeBoolean(true);
        ProtocolUtils.writeVarInt(buf, value.material());
      } else {
        buf.writeShort(value.material());
      }

      buf.writeByte(value.amount());
      if (hasDamage) {
        buf.writeShort(value.data());
      }

      LimboProtocolUtils.writeCompoundBinaryTag(buf, version, value.nbt());
    }
  }

  public static BinaryTag readCompoundTag(ByteBuf buf, ProtocolVersion version) {
    CompoundBinaryTag result = LimboProtocolUtils.readCompoundTagOrNull(buf, version);
    return result == null ? EndBinaryTag.endBinaryTag() : result;
  }

  public static CompoundBinaryTag readCompoundTagOrNull(ByteBuf buf, ProtocolVersion version) {
    BinaryTag binaryTag = LimboProtocolUtils.readBinaryTagOrNull(buf, version);
    if (binaryTag == null) {
      return null;
    }

    if (binaryTag.type() != BinaryTagTypes.COMPOUND) {
      throw new DecoderException("Expected root tag to be CompoundTag, but is " + binaryTag.getClass().getSimpleName());
    }

    return (CompoundBinaryTag) binaryTag;
  }

  public static BinaryTag readBinaryTag(ByteBuf buf, ProtocolVersion version) {
    BinaryTag result = LimboProtocolUtils.readBinaryTagOrNull(buf, version);
    return result == null ? EndBinaryTag.endBinaryTag() : result;
  }

  public static BinaryTag readBinaryTagOrNull(ByteBuf buf, ProtocolVersion version) {
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      short length = buf.readShort();
      if (length < 0) {
        return null;
      } else {
        byte[] data = new byte[length];
        buf.readBytes(data);
        try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(data))))) {
          BinaryTagType<?> type = LimboProtocolUtils.BINARY_TAG_TYPES[inputStream.readByte()];
          buf.skipBytes(buf.readUnsignedShort());
          return type.read(inputStream);
        } catch (IOException e) {
          throw new DecoderException(e);
        }
      }
    } else {
      byte type = buf.readByte();
      if (type <= 0) {
        return null;
      } else {
        if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
          buf.skipBytes(buf.readUnsignedShort());
        }

        try {
          return LimboProtocolUtils.BINARY_TAG_TYPES[type].read(new ByteBufInputStream(buf));
        } catch (IOException thrown) {
          throw new DecoderException("Unable to parse BinaryTag, full error: " + thrown.getMessage());
        }
      }
    }
  }

  public static void writeCompoundBinaryTag(ByteBuf buf, ProtocolVersion version, BinaryTag tag) {
    LimboProtocolUtils.writeBinaryTag(buf, version, tag == EndBinaryTag.endBinaryTag() ? null : tag);
  }

  @SuppressWarnings("unchecked")
  public static <T extends BinaryTag> void writeBinaryTag(ByteBuf buf, ProtocolVersion version, T tag) {
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      if (tag == null) {
        buf.writeShort(-1);
      } else {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
          try (DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(output))) {
            BinaryTagType<T> type = (BinaryTagType<T>) tag.type();
            outputStream.writeByte(type.id());
            outputStream.writeShort(0); // writeUTF("")
            type.write(tag, outputStream);
          }

          byte[] result = output.toByteArray();
          buf.writeShort((short) result.length);
          buf.writeBytes(result);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } else if (tag == null) {
      buf.writeByte(0);
    } else {
      ProtocolUtils.writeBinaryTag(buf, version, tag);
    }
  }

  @FunctionalInterface
  public interface EncoderByteBufValue<T> {

    void write(ByteBuf buf, T value);
  }

  @FunctionalInterface
  public interface EncoderValueByteBuf<T> {

    void write(T value, ByteBuf buf);
  }
}
