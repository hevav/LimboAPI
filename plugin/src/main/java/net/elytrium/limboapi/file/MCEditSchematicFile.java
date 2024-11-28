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

import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.WorldFile;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class MCEditSchematicFile implements WorldFile {

  private final short width;
  private final short height;
  private final short length;
  private final byte[] blocks;
  private final byte[] addBlocks;

  public MCEditSchematicFile(CompoundBinaryTag tag) {
    this.width = tag.getShort("Width");
    this.height = tag.getShort("Height");
    this.length = tag.getShort("Length");
    this.blocks = tag.getByteArray("Blocks");
    BinaryTag addBlocks = tag.get("AddBlocks");
    this.addBlocks = addBlocks == null ? new byte[0] : ((ByteArrayBinaryTag) addBlocks).value();
  }

  @Override
  public void toWorld(LimboFactory factory, VirtualWorld world, int offsetX, int offsetY, int offsetZ, int lightLevel) {
    short[] blockIds = new short[this.blocks.length];
    for (int index = 0; index < blockIds.length; ++index) {
      if ((index >> 1) >= this.addBlocks.length) {
        blockIds[index] = (short) (this.blocks[index] & 0xFF);
      } else if ((index & 1) == 0) {
        blockIds[index] = (short) (((this.addBlocks[index >> 1] & 0x0F) << 8) + (this.addBlocks[index] & 0xFF));
      } else {
        blockIds[index] = (short) (((this.addBlocks[index >> 1] & 0xF0) << 4) + (this.addBlocks[index] & 0xFF));
      }
    }

    for (int posX = 0; posX < this.width; ++posX) {
      for (int posY = 0; posY < this.height; ++posY) {
        for (int posZ = 0; posZ < this.length; ++posZ) {
          int index = (posY * this.length + posZ) * this.width + posX;
          world.setBlock(posX + offsetX, posY + offsetY, posZ + offsetZ, factory.createSimpleBlock(blockIds[index]));
        }
      }
    }

    world.fillSkyLight(lightLevel);
  }
}
