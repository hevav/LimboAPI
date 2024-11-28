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

import com.google.common.base.Preconditions;
import net.elytrium.limboapi.api.chunk.data.LightSection;
import net.elytrium.limboapi.api.mcprotocollib.NibbleArray3D;

public class SimpleLightSection implements LightSection {

  private static final NibbleArray3D NO_LIGHT = new NibbleArray3D(SimpleChunk.MAX_BLOCKS_PER_SECTION);
  private static final NibbleArray3D ALL_LIGHT = new NibbleArray3D(SimpleChunk.MAX_BLOCKS_PER_SECTION, 15);

  private NibbleArray3D blockLight;
  private NibbleArray3D skyLight;

  public SimpleLightSection() {
    this(NO_LIGHT, ALL_LIGHT);
  }

  private SimpleLightSection(NibbleArray3D blockLight, NibbleArray3D skyLight) {
    this.blockLight = blockLight;
    this.skyLight = skyLight;
  }

  @Override
  public void setBlockLight(int posX, int posY, int posZ, byte light) {
    this.checkIndexes(posX, posY, posZ);
    Preconditions.checkArgument(light >= 0 && light <= 15, "light should be between 0 and 15");

    if (this.blockLight == NO_LIGHT && light != 0) {
      this.blockLight = new NibbleArray3D(SimpleChunk.MAX_BLOCKS_PER_SECTION);
    }

    this.blockLight.set(posX, posY, posZ, light);
  }

  @Override
  public NibbleArray3D getBlockLight() {
    return this.blockLight;
  }

  @Override
  public byte getBlockLight(int posX, int posY, int posZ) {
    this.checkIndexes(posX, posY, posZ);
    return (byte) this.blockLight.get(posX, posY, posZ);
  }

  @Override
  public void setSkyLight(int posX, int posY, int posZ, byte light) {
    this.checkIndexes(posX, posY, posZ);
    Preconditions.checkArgument(light >= 0 && light <= 15, "light should be between 0 and 15");

    if (this.skyLight == ALL_LIGHT && light != 15) {
      this.skyLight = new NibbleArray3D(SimpleChunk.MAX_BLOCKS_PER_SECTION);
    }

    this.skyLight.set(posX, posY, posZ, light);
  }

  @Override
  public NibbleArray3D getSkyLight() {
    return this.skyLight;
  }

  @Override
  public byte getSkyLight(int posX, int posY, int posZ) {
    this.checkIndexes(posX, posY, posZ);
    return (byte) this.skyLight.get(posX, posY, posZ);
  }

  private void checkIndexes(int posX, int posY, int posZ) {
    Preconditions.checkArgument(this.checkIndex(posX), "x should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(posY), "y should be between 0 and 15");
    Preconditions.checkArgument(this.checkIndex(posZ), "z should be between 0 and 15");
  }

  private boolean checkIndex(int pos) {
    return pos >= 0 && pos <= 15;
  }

  @Override
  public SimpleLightSection copy() {
    NibbleArray3D skyLight = this.skyLight == ALL_LIGHT ? ALL_LIGHT : this.skyLight.copy();
    NibbleArray3D blockLight = this.blockLight == NO_LIGHT ? NO_LIGHT : this.blockLight.copy();
    return new SimpleLightSection(blockLight, skyLight);
  }
}
