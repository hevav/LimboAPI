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
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.api.chunk.util.CompactStorage;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.protocol.packets.data.BiomeData;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.mcprotocollib.BitStorage116;
import net.elytrium.limboapi.mcprotocollib.BitStorage19;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.protocol.util.NetworkSection;
import net.elytrium.limboapi.server.world.SimpleBlockEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;

// TODO fix 1.17.x spawn with offset -1 by y (i guess the client tries to fall before chunks are loaded, and as a result, due to the ping, the player may spawn inside the blocks)
public class ChunkDataPacket implements MinecraftPacket {

  private final ChunkSnapshot chunk;
  private final NetworkSection[] sections;
  private final int availableSections;
  private final int skyLightMask;
  private final int maxSections;
  private final int nonNullSections;
  private final BiomeData biomeData;
  private final CompoundBinaryTag heightmap114;
  private final CompoundBinaryTag heightmap116;
  private final boolean hasSkyLight;

  private List<VirtualBlockEntity.Entry> flowerPots;

  public ChunkDataPacket(ChunkSnapshot chunk, boolean hasSkyLight, int maxSections) {
    this(chunk, hasSkyLight, maxSections, List.of());
  }

  public ChunkDataPacket(ChunkSnapshot chunk, boolean hasSkyLight, int maxSections, List<VirtualBlockEntity.Entry> flowerPots) {
    this.chunk = chunk;
    this.sections = new NetworkSection[maxSections];

    boolean fullChunk = chunk.fullChunk();
    int availableSections = 0;
    int skyLightMask = fullChunk ? 0x3FFFF : 0;
    int nonNullSections = 0;
    BlockSection[] sections = chunk.sections();
    LightSection[] light = chunk.light();
    VirtualBiome[] biomes = chunk.biomes();
    for (int i = 0; i < sections.length; ++i) {
      BlockSection section = sections[i];
      if (section != null) {
        {
          availableSections |= 1 << i;
          if (!fullChunk && hasSkyLight) {
            skyLightMask |= (1 << i);
          }
        }
        ++nonNullSections;
        LightSection lightSection = light[i];
        this.sections[i] = new NetworkSection(i, section, lightSection.getBlockLight(), hasSkyLight ? lightSection.getSkyLight() : null, biomes);
      }
    }

    this.availableSections = availableSections;
    this.skyLightMask = skyLightMask;
    this.maxSections = maxSections;
    this.nonNullSections = nonNullSections;
    this.biomeData = new BiomeData(chunk);
    this.heightmap114 = this.createHeightMap(true);
    this.heightmap116 = this.createHeightMap(false);
    this.hasSkyLight = hasSkyLight;
    this.flowerPots = flowerPots;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    boolean fullChunk = this.chunk.fullChunk();
    if (!fullChunk && protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      throw new IllegalStateException(">=1.17 supports only full chunks");
    }

    buf.writeLong(((long) this.chunk.posX() & 0xFFFFFFFFL) << 32 | (long) this.chunk.posZ() & 0xFFFFFFFFL);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      if (protocolVersion == ProtocolVersion.MINECRAFT_1_17 || protocolVersion == ProtocolVersion.MINECRAFT_1_17_1) {
        ProtocolUtils.writeVarInt(buf, 1); // length
        buf.writeLong(this.availableSections);
      }
    } else {
      buf.writeBoolean(fullChunk);

      if (protocolVersion == ProtocolVersion.MINECRAFT_1_16 || protocolVersion == ProtocolVersion.MINECRAFT_1_16_1) {
        buf.writeBoolean(true); // forgetOldData
      }

      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
        ProtocolUtils.writeVarInt(buf, this.availableSections);
      } else {
        // <=1.8 client doesn't treat void-alike chunks as loaded and do slow fall
        // We are changing void-sections count here, and the client thinks that the chunk is loaded
        buf.writeShort(this.availableSections == 0 ? 1 : this.availableSections);
      }
    }

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_16)) {
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, this.heightmap114);
      } else {
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, this.heightmap116);
      }
    }

    // 1.15 - 1.17.1 biomes
    if (fullChunk && protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_15) && protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_17_1)) {
      int[] post115Biomes = this.biomeData.getPost115Biomes();
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        LimboProtocolUtils.writeVarIntArray(buf, post115Biomes);
      } else {
        for (int value : post115Biomes) {
          buf.writeInt(value);
        }
      }
    }

    ByteBuf data = this.createChunkData(protocolVersion);
    try {
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        ProtocolUtils.writeVarInt(buf, data.readableBytes());
        buf.writeBytes(data);
        if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_9_4)) {
          Consumer<VirtualBlockEntity.Entry> encoder = entry -> {
            if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
              buf.writeByte(((entry.getPosX() & 0x0F) << 4) | (entry.getPosZ() & 0x0F));
              buf.writeShort(entry.getPosY());
              var blockEntity = entry.getBlockEntity();
              int id = blockEntity.getId(protocolVersion);
              if (id == Integer.MIN_VALUE) {
                LimboAPI.getLogger().warn("Could not find block entity id '{}' for {}", blockEntity.getModernId(), protocolVersion);
                ProtocolUtils.writeVarInt(buf, 0);
              } else {
                ProtocolUtils.writeVarInt(buf, id);
              }
            }

            ProtocolUtils.writeBinaryTag(buf, protocolVersion, entry.getNbt(protocolVersion));
          };
          VirtualBlockEntity.Entry[] array = this.chunk.blockEntityEntries(protocolVersion);
          List<VirtualBlockEntity.Entry> flowerPots = null;
          if (protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
            flowerPots = this.flowerPots;
            if (flowerPots == null) {
              this.flowerPots = flowerPots = ChunkDataPacket.getAdditionalFlowerPots(this.chunk);
            }
          }
          ProtocolUtils.writeVarInt(buf, flowerPots == null ? array.length : array.length + flowerPots.size());
          for (VirtualBlockEntity.Entry entry : array) {
            encoder.accept(entry);
          }

          if (flowerPots != null) {
            flowerPots.forEach(encoder);
          }
        }

        if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
          ChunkDataPacket.writeLight(buf, protocolVersion, this.chunk.light(), this.skyLightMask, this.availableSections);
        }
      } else {
        ChunkDataPacket.write17(buf, data);
      }
    } finally {
      data.release();
    }
  }

  static void writeLight(ByteBuf buf, ProtocolVersion protocolVersion, LightSection[] light, int skyLightMask, int blockLightMask) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_16) && protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      buf.writeBoolean(true); // Trust edges
    }

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      // Skylight mask
      ProtocolUtils.writeVarInt(buf, 1); // length
      buf.writeLong(skyLightMask);

      // BlockLight mask
      ProtocolUtils.writeVarInt(buf, 1); // length
      buf.writeLong(blockLightMask);
    } else {
      ProtocolUtils.writeVarInt(buf, skyLightMask);
      ProtocolUtils.writeVarInt(buf, blockLightMask);
    }

    ProtocolUtils.writeVarInt(buf, 0); // EmptySkylight mask
    ProtocolUtils.writeVarInt(buf, 0); // EmptyBlockLight mask

    ChunkDataPacket.writeLight(buf, protocolVersion, light, skyLightMask, section -> ProtocolUtils.writeByteArray(buf, section.getSkyLight().getData()));
    ChunkDataPacket.writeLight(buf, protocolVersion, light, blockLightMask, section -> ProtocolUtils.writeByteArray(buf, section.getBlockLight().getData()));
  }

  private static void writeLight(ByteBuf buf, ProtocolVersion protocolVersion, LightSection[] light, int mask, Consumer<LightSection> encoder) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      LimboProtocolUtils.writeArray(buf, light, encoder);
    } else {
      for (int i = 0, length = light.length; i < length; ++i) {
        if ((mask & 1 << i) != 0) {
          encoder.accept(light[i]);
        }
      }
    }
  }

  private ByteBuf createChunkData(ProtocolVersion version) {
    int dataLength = 0;
    for (NetworkSection networkSection : this.sections) {
      if (networkSection != null) {
        dataLength += networkSection.getDataLength(version);
      }
    }

    boolean hasBiomeData = this.chunk.fullChunk() && version.noGreaterThan(ProtocolVersion.MINECRAFT_1_14_4);
    if (hasBiomeData) {
      dataLength += version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2) ? 256 : 256 * 4;
    }

    if (this.availableSections == 0 && version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
      dataLength += version == ProtocolVersion.MINECRAFT_1_8
          ? (this.hasSkyLight ? (4096 * Short.BYTES) + 2048 + 2048 : (4096 * Short.BYTES) + 2048)
          : (this.hasSkyLight ? 4096 + 2048 + 2048 + 2048 : 4096 + 2048 + 2048);
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
      dataLength += (this.maxSections - this.nonNullSections) * (Short.BYTES + Byte.BYTES + 1 + 1 + Byte.BYTES + 1 + 1);
    }

    ByteBuf data = Unpooled.buffer(dataLength);
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_8)) {
      if (this.availableSections == 0) {
        // Since we changed the number of available sections to 1, we need to write one additional empty section
        data.writeZero(version == ProtocolVersion.MINECRAFT_1_8
            ? this.hasSkyLight ? (4096 * Short.BYTES)/*blocks*/ + 2048/*blocklight*/ + 2048/*skylight*/ : (4096 * Short.BYTES)/*blocks*/ + 2048/*blocklight*/
            : this.hasSkyLight ? 4096/*blocks*/ + 2048/*metadata*/ + 2048/*blocklight*/ + 2048/*skylight*/ : 4096/*blocks*/ + 2048/*metadata*/ + 2048/*blocklight*/);
      } else {
        for (int pass = 0; pass < 4; ++pass) {
          for (NetworkSection section : this.sections) {
            if (section != null) {
              section.write17Data(data, version, pass);
            }
          }
        }
      }
    } else {
      for (NetworkSection section : this.sections) {
        if (section != null) {
          section.writeData(data, version);
        } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
          data.writeShort(0); // Block count = 0
          data.writeByte(0); // BlockStorage: 0 bit per entry = Single palette
          ProtocolUtils.writeVarInt(data, Block.AIR.getId()); // Only air block in the palette
          ProtocolUtils.writeVarInt(data, 0); // BlockStorage: 0 entries
          data.writeByte(0); // BiomeStorage: 0 bit per entry = Single palette
          ProtocolUtils.writeVarInt(data, Biome.PLAINS.getId()); // Only Plain biome in the palette
          ProtocolUtils.writeVarInt(data, 0); // BiomeStorage: 0 entries
        }
      }
    }

    if (hasBiomeData) {
      for (int value : this.biomeData.getPre115Biomes()) {
        if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
          data.writeByte(value);
        } else {
          data.writeInt(value);
        }
      }
    }

    if (dataLength != data.readableBytes()) {
      LimboAPI.getLogger().warn("Data length mismatch: got: {}, expected: {}, version: {}", dataLength, data.readableBytes(), version);
    }

    return data;
  }

  private CompoundBinaryTag createHeightMap(boolean pre116) {
    CompactStorage motionBlocking = pre116 ? new BitStorage19(9, 256) : new BitStorage116(9, 256);
    CompactStorage surface = pre116 ? new BitStorage19(9, 256) : new BitStorage116(9, 256);
    // TODO better way (?) (я уже не помню что я имел ввиду)
    for (int posY = 0; posY < 256; ++posY) {
      for (int posX = 0; posX < 16; ++posX) {
        for (int posZ = 0; posZ < 16; ++posZ) {
          VirtualBlock block = this.chunk.getBlock(posX, posY, posZ);
          if (block.motionBlocking()) {
            motionBlocking.set(posX + (posZ << 4), posY + 1);
          }

          if (!block.air()) {
            surface.set(posX + (posZ << 4), posY + 1);
          }
        }
      }
    }

    return CompoundBinaryTag.builder()
        .putLongArray("MOTION_BLOCKING", motionBlocking.getData())
        .putLongArray("WORLD_SURFACE", surface.getData())
        .build();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }

  @Override
  public String toString() {
    return "ChunkDataPacket{"
           + "chunk=" + this.chunk
           + ", sections=" + Arrays.toString(this.sections)
           + ", availableSections=" + this.availableSections
           + ", maxSections=" + this.maxSections
           + ", nonNullSections=" + this.nonNullSections
           + ", biomeData=" + this.biomeData
           + ", heightmap114=" + this.heightmap114
           + ", heightmap116=" + this.heightmap116
           + "}";
  }

  private static void write17(ByteBuf out, ByteBuf data) {
    out.writeShort(0); // Extended bitmask
    try (var compressor = Natives.compress.get().create(6)) {
      ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(data.alloc(), compressor, data);
      try {
        out.writeInt(0);
        int preIndex = out.writerIndex();
        compressor.deflate(compatibleIn, out);
        int postIndex = out.writerIndex();
        out.writerIndex(preIndex - 4);
        out.writeInt(postIndex - preIndex);
        out.writerIndex(postIndex);
      } catch (DataFormatException e) {
        throw new RuntimeException(e);
      } finally {
        compatibleIn.release();
      }
    }
  }

  // In <=1.12.2 flower pots are still block entities, while on higher versions it's just blockstates
  public static List<VirtualBlockEntity.Entry> getAdditionalFlowerPots(ChunkSnapshot chunk) {
    List<VirtualBlockEntity.Entry> flowerPots = null;
    VirtualBlockEntity flowerPot = null;
    BlockSection[] sections = chunk.sections();
    for (int i = 0, length = sections.length; i < length; ++i) {
      BlockSection section = sections[i];
      if (section == null) {
        continue;
      }

      for (int posY = 0; posY < 16; ++posY) {
        for (int posX = 0; posX < 16; ++posX) {
          for (int posZ = 0; posZ < 16; ++posZ) {
            short id = section.getBlockAt(posX, posY, posZ).blockStateId(ProtocolVersion.MINECRAFT_1_13);
            if (id >= 5265 && id <= 5286) {
              CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
              switch (id) {
                case 5265 -> {
                  builder.putString("Item", "minecraft:air");
                  builder.putInt("Data", 0);
                }
                case 5266 -> {
                  builder.putString("Item", "minecraft:sapling");
                  builder.putInt("Data", 0);
                }
                case 5267 -> {
                  builder.putString("Item", "minecraft:sapling");
                  builder.putInt("Data", 1);
                }
                case 5268 -> {
                  builder.putString("Item", "minecraft:sapling");
                  builder.putInt("Data", 2);
                }
                case 5269 -> {
                  builder.putString("Item", "minecraft:sapling");
                  builder.putInt("Data", 3);
                }
                case 5270 -> {
                  builder.putString("Item", "minecraft:sapling");
                  builder.putInt("Data", 4);
                }
                case 5271 -> {
                  builder.putString("Item", "minecraft:sapling");
                  builder.putInt("Data", 5);
                }
                case 5272 -> {
                  builder.putString("Item", "minecraft:tallgrass");
                  builder.putInt("Data", 2);
                }
                case 5273 -> {
                  builder.putString("Item", "minecraft:yellow_flower");
                  builder.putInt("Data", 0);
                }
                case 5274 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 0);
                }
                case 5275 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 1);
                }
                case 5276 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 2);
                }
                case 5277 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 3);
                }
                case 5278 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 4);
                }
                case 5279 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 5);
                }
                case 5280 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 6);
                }
                case 5281 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 7);
                }
                case 5282 -> {
                  builder.putString("Item", "minecraft:red_flower");
                  builder.putInt("Data", 8);
                }
                case 5283 -> {
                  builder.putString("Item", "minecraft:red_mushroom");
                  builder.putInt("Data", 0);
                }
                case 5284 -> {
                  builder.putString("Item", "minecraft:brown_mushroom");
                  builder.putInt("Data", 0);
                }
                case 5285 -> {
                  builder.putString("Item", "minecraft:deadbush");
                  builder.putInt("Data", 0);
                }
                case 5286 -> {
                  builder.putString("Item", "minecraft:cactus");
                  builder.putInt("Data", 0);
                }
              }
              if (flowerPots == null) {
                flowerPots = new ArrayList<>();
                flowerPot = SimpleBlockEntity.fromModernId("minecraft:flower_pot");
                if (flowerPot == null) {
                  throw new NullPointerException();
                }
              }

              flowerPots.add(flowerPot.createEntry(null, (chunk.posX() << 4) + posX, posY + (i << 4), (chunk.posZ() << 4) + posZ, builder.build()));
            }
          }
        }
      }
    }

    return flowerPots;
  }
}
