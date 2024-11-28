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

package net.elytrium.limboapi.file;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.WorldFile;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

public class WorldEditSchemFile implements WorldFile {

  private final short width;
  private final short height;
  private final short length;
  private final int[] blocks;
  private final CompoundBinaryTag palette;
  private final ListBinaryTag blockEntities;

  public WorldEditSchemFile(CompoundBinaryTag tag) {
    this.width = tag.getShort("Width");
    this.height = tag.getShort("Height");
    this.length = tag.getShort("Length");
    this.palette = tag.getCompound("Palette");

    ByteBuf blockDataBuf = Unpooled.wrappedBuffer(tag.getByteArray("BlockData"));
    this.blocks = new int[this.width * this.height * this.length];
    for (int i = 0; i < this.blocks.length; ++i) {
      this.blocks[i] = ProtocolUtils.readVarInt(blockDataBuf);
    }
    blockDataBuf.release();

    this.blockEntities = tag.getList("BlockEntities");
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ, int lightLevel) {
    VirtualBlock[] palettedBlocks = new VirtualBlock[this.palette.keySet().size()];
    this.palette.forEach(entry -> palettedBlocks[((IntBinaryTag) entry.getValue()).value()] = factory.createSimpleBlock(entry.getKey()));

    for (int posX = 0; posX < this.width; ++posX) {
      for (int posY = 0; posY < this.height; ++posY) {
        for (int posZ = 0; posZ < this.length; ++posZ) {
          world.setBlock(offsetX + posX, offsetY + posY, offsetZ + posZ, palettedBlocks[this.blocks[(posY * this.length + posZ) * this.width + posX]]);
        }
      }
    }

    for (BinaryTag blockEntity : this.blockEntities) {
      CompoundBinaryTag blockEntityData = (CompoundBinaryTag) blockEntity;
      int[] posTag = blockEntityData.getIntArray("Pos");
      world.setBlockEntity(offsetX + posTag[0], offsetY + posTag[1], offsetZ + posTag[2], blockEntityData, factory.getBlockEntityFromModernId(blockEntityData.getString("Id")));
    }

    world.fillSkyLight(lightLevel);
  }
}
