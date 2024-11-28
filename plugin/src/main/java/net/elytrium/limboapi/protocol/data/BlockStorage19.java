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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.data.BlockStorage;
import net.elytrium.limboapi.api.chunk.util.CompactStorage;
import net.elytrium.limboapi.mcprotocollib.BitStorage116;
import net.elytrium.limboapi.mcprotocollib.BitStorage19;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BlockStorage19 implements BlockStorage {

  private static final int MIN_BITS_PER_ENTRY = 4;
  private static final int MAX_BITS_PER_ENTRY = 8;

  private final ProtocolVersion version;
  private final List<VirtualBlock> palette;
  private final Map<Short, VirtualBlock> rawToBlock;

  private CompactStorage storage;

  public BlockStorage19(ProtocolVersion version) {
    this.version = version;
    this.palette = new ArrayList<>();
    this.rawToBlock = new HashMap<>();

    this.palette.add(SimpleBlock.AIR);
    this.rawToBlock.put(SimpleBlock.AIR.blockStateId(version), SimpleBlock.AIR);

    this.storage = this.createStorage(BlockStorage19.MIN_BITS_PER_ENTRY);
  }

  private BlockStorage19(ProtocolVersion version, List<VirtualBlock> palette, Map<Short, VirtualBlock> rawToBlock, CompactStorage storage) {
    this.version = version;
    this.palette = palette;
    this.rawToBlock = rawToBlock;
    this.storage = storage;
  }

  @Override
  public void write(Object byteBufObject, ProtocolVersion version, int pass) {
    Preconditions.checkArgument(byteBufObject instanceof ByteBuf);
    ByteBuf buf = (ByteBuf) byteBufObject;
    buf.writeByte(this.storage.getBitsPerEntry());
    if (this.storage.getBitsPerEntry() > BlockStorage19.MAX_BITS_PER_ENTRY) {
      if (this.version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
        ProtocolUtils.writeVarInt(buf, 0);
      }
    } else {
      LimboProtocolUtils.writeCollection(buf, this.palette, state -> ProtocolUtils.writeVarInt(buf, state.blockStateId(this.version)));
    }

    this.storage.write(buf, version);
  }

  @Override
  public void set(int posX, int posY, int posZ, @NonNull VirtualBlock block) {
    int id = this.getId(block);
    this.storage.set(BlockStorage.index(posX, posY, posZ), id);
  }

  private int getId(VirtualBlock block) {
    int bitsPerEntry = this.storage.getBitsPerEntry();
    if (bitsPerEntry > BlockStorage19.MAX_BITS_PER_ENTRY) {
      short raw = block.blockStateId(this.version);
      this.rawToBlock.put(raw, block);
      return raw;
    } else {
      int id = this.palette.indexOf(block);
      if (id == -1) {
        int size = this.palette.size();
        if (size >= (1 << bitsPerEntry)) {
          ++bitsPerEntry;
          CompactStorage newStorage = this.createStorage(bitsPerEntry);
          for (int i = 0; i < SimpleChunk.MAX_BLOCKS_PER_SECTION; ++i) {
            newStorage.set(i, bitsPerEntry > BlockStorage19.MAX_BITS_PER_ENTRY ? this.palette.get(this.storage.get(i)).blockStateId(this.version) : this.storage.get(i));
          }

          this.storage = newStorage;
          return this.getId(block);
        }

        this.palette.add(block);
        id = size;
      }

      return id;
    }
  }

  private CompactStorage createStorage(int bitsPerEntry) {
    return this.version.lessThan(ProtocolVersion.MINECRAFT_1_16)
        ? new BitStorage19(bitsPerEntry, SimpleChunk.MAX_BLOCKS_PER_SECTION)
        : new BitStorage116(bitsPerEntry, SimpleChunk.MAX_BLOCKS_PER_SECTION);
  }

  @NonNull
  @Override
  public VirtualBlock get(int posX, int posY, int posZ) {
    int id = this.storage.get(BlockStorage.index(posX, posY, posZ));
    return this.storage.getBitsPerEntry() > BlockStorage19.MAX_BITS_PER_ENTRY ? this.rawToBlock.get((short) id) : this.palette.get(id);
  }

  @Override
  public int getDataLength(ProtocolVersion version) {
    int length = 1;
    if (this.storage.getBitsPerEntry() > BlockStorage19.MAX_BITS_PER_ENTRY) {
      if (this.version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2)) {
        length += 1;
      }
    } else {
      length += ProtocolUtils.varIntBytes(this.palette.size());
      for (VirtualBlock state : this.palette) {
        length += ProtocolUtils.varIntBytes(state.blockStateId(this.version));
      }
    }

    return length + this.storage.getDataLength();
  }

  @Override
  public BlockStorage copy() {
    return new BlockStorage19(this.version, new ArrayList<>(this.palette), new HashMap<>(this.rawToBlock), this.storage.copy());
  }

  @Override
  public String toString() {
    return "BlockStorage19{"
        + "version=" + this.version
        + ", palette=" + this.palette
        + ", rawToBlock=" + this.rawToBlock
        + ", storage=" + this.storage
        + "}";
  }
}
