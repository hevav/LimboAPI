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

package net.elytrium.limboapi.protocol.data;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3D;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BlockStorage17 implements BlockStorage {

  private final VirtualBlock[] blocks;

  public BlockStorage17() {
    this(new VirtualBlock[SimpleChunk.MAX_BLOCKS_PER_SECTION]);
  }

  private BlockStorage17(VirtualBlock[] blocks) {
    this.blocks = blocks;
  }

  @Override
  public void write(Object byteBufObject, ProtocolVersion version, int pass) {
    Preconditions.checkArgument(byteBufObject instanceof ByteBuf);
    ByteBuf buf = (ByteBuf) byteBufObject;
    if (pass == 0) {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        byte[] raw = new byte[this.blocks.length];
        for (int i = 0; i < this.blocks.length; ++i) {
          VirtualBlock block = this.blocks[i];
          raw[i] = (byte) (block == null ? 0 : block.blockStateId(ProtocolVersion.MINECRAFT_1_7_2) >> 4);
        }

        buf.writeBytes(raw);
      } else {
        short[] raw = new short[this.blocks.length];
        for (int i = 0; i < this.blocks.length; ++i) {
          VirtualBlock block = this.blocks[i];
          raw[i] = (short) (block == null ? 0 : block.blockStateId(ProtocolVersion.MINECRAFT_1_8));
        }

        for (short value : raw) {
          buf.writeShortLE(value);
        }
      }
    } else if (pass == 1 && version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      NibbleArray3D metadata = new NibbleArray3D(SimpleChunk.MAX_BLOCKS_PER_SECTION);
      for (int i = 0; i < this.blocks.length; ++i) {
        VirtualBlock block = this.blocks[i];
        metadata.set(i, block == null ? 0 : block.blockStateId(ProtocolVersion.MINECRAFT_1_7_2) & 0xFFFF);
      }

      buf.writeBytes(metadata.getData());
    }
  }

  @Override
  public void set(int posX, int posY, int posZ, @NonNull VirtualBlock block) {
    this.blocks[BlockStorage.index(posX, posY, posZ)] = block;
  }

  @NonNull
  @Override
  public VirtualBlock get(int posX, int posY, int posZ) {
    VirtualBlock block = this.blocks[BlockStorage.index(posX, posY, posZ)];
    return block == null ? SimpleBlock.AIR : block;
  }

  @Override
  public int getDataLength(ProtocolVersion version) {
    return version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6) ? this.blocks.length + (SimpleChunk.MAX_BLOCKS_PER_SECTION >> 1) : this.blocks.length * Short.BYTES;
  }

  @Override
  public BlockStorage copy() {
    return new BlockStorage17(Arrays.copyOf(this.blocks, this.blocks.length));
  }

  @Override
  public String toString() {
    return "BlockStorage17{"
        + "blocks=" + Arrays.toString(this.blocks)
        + "}";
  }
}
