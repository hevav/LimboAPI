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

package net.elytrium.limboapi.server.world.chunk;

import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.data.BlockSection;
import net.elytrium.limboapi.api.chunk.data.ChunkSnapshot;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.server.world.SimpleBlock;

public record SimpleChunkSnapshot(int posX, int posZ, boolean fullChunk,
    BlockSection[] sections, LightSection[] light, VirtualBiome[] biomes, VirtualBlockEntity.Entry[] blockEntityEntries) implements ChunkSnapshot {

  @Override
  public VirtualBlock getBlock(int posX, int posY, int posZ) {
    BlockSection section = this.sections[posY >> 4];
    return section == null ? SimpleBlock.AIR : section.getBlockAt(posX & 0x0F, posY & 0x0F, posZ & 0x0F);
  }
}
