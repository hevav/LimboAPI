/*
 * This file is part of MCProtocolLib, licensed under the MIT License (MIT).
 *
 * Copyright (C) 2013-2021 Steveice10
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.elytrium.limboapi.mcprotocollib;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.elytrium.limboapi.api.chunk.util.CompactStorage;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;

public class BitStorage116 implements CompactStorage {

  private static final int[] MAGIC = {
      -1, -1, 0, Integer.MIN_VALUE, 0, 0, 1431655765, 1431655765, 0, Integer.MIN_VALUE, 0, 1, 858993459, 858993459, 0, 715827882, 715827882, 0, 613566756, 613566756, 0, Integer.MIN_VALUE, 0, 2, 477218588,
      477218588, 0, 429496729, 429496729, 0, 390451572, 390451572, 0, 357913941, 357913941, 0, 330382099, 330382099, 0, 306783378, 306783378, 0, 286331153, 286331153, 0, Integer.MIN_VALUE, 0, 3, 252645135,
      252645135, 0, 238609294, 238609294, 0, 226050910, 226050910, 0, 214748364, 214748364, 0, 204522252, 204522252, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 178956970, 178956970, 0, 171798691,
      171798691, 0, 165191049, 165191049, 0, 159072862, 159072862, 0, 153391689, 153391689, 0, 148102320, 148102320, 0, 143165576, 143165576, 0, 138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524,
      130150524, 0, 126322567, 126322567, 0, 122713351, 122713351, 0, 119304647, 119304647, 0, 116080197, 116080197, 0, 113025455, 113025455, 0, 110127366, 110127366, 0, 107374182, 107374182, 0, 104755299,
      104755299, 0, 102261126, 102261126, 0, 99882960, 99882960, 0, 97612893, 97612893, 0, 95443717, 95443717, 0, 93368854, 93368854, 0, 91382282, 91382282, 0, 89478485, 89478485, 0, 87652393, 87652393, 0,
      85899345, 85899345, 0, 84215045, 84215045, 0, 82595524, 82595524, 0, 81037118, 81037118, 0, 79536431, 79536431, 0, 78090314, 78090314, 0, 76695844, 76695844, 0, 75350303, 75350303, 0, 74051160,
      74051160, 0, 72796055, 72796055, 0, 71582788, 71582788, 0, 70409299, 70409299, 0, 69273666, 69273666, 0, 68174084, 68174084, 0, Integer.MIN_VALUE, 0, 5
  };

  private final long[] data;
  private final int bitsPerEntry;
  private final int size;

  private final long maxValue;
  private final int valuesPerLong;
  private final long divideMultiply;
  private final long divideAdd;
  private final int divideShift;

  public BitStorage116(int bitsPerEntry, int size) {
    this(bitsPerEntry, size, null);
  }

  public BitStorage116(int bitsPerEntry, int size, long[] data) {
    if (bitsPerEntry < 1 || bitsPerEntry > 32) {
      throw new IllegalArgumentException("bitsPerEntry must be between 1 and 32, inclusive");
    }

    this.bitsPerEntry = bitsPerEntry;
    this.size = size;

    this.maxValue = (1L << bitsPerEntry) - 1L;
    this.valuesPerLong = (char) (64 / bitsPerEntry);
    int expectedLength = (size + this.valuesPerLong - 1) / this.valuesPerLong;
    if (data == null) {
      this.data = new long[expectedLength];
    } else {
      if (data.length != expectedLength) {
        throw new IllegalArgumentException("Expected " + expectedLength + " longs but got " + data.length + " longs");
      }

      this.data = data;
    }

    int magicIndex = 3 * (this.valuesPerLong - 1);
    this.divideMultiply = Integer.toUnsignedLong(BitStorage116.MAGIC[magicIndex]);
    this.divideAdd = Integer.toUnsignedLong(BitStorage116.MAGIC[magicIndex + 1]);
    this.divideShift = BitStorage116.MAGIC[magicIndex + 2];
  }

  @Override
  public void set(int index, int value) {
    if (index < 0 || index > this.size - 1) {
      throw new IndexOutOfBoundsException();
    } else if (value < 0 || value > this.maxValue) {
      throw new IllegalArgumentException("Value cannot be outside of accepted range");
    } else {
      int cellIndex = this.cellIndex(index);
      int bitIndex = this.bitIndex(index, cellIndex);
      this.data[cellIndex] = this.data[cellIndex] & ~(this.maxValue << bitIndex) | ((long) value & this.maxValue) << bitIndex;
    }
  }

  @Override
  public int get(int index) {
    if (index < 0 || index > this.size - 1) {
      throw new IndexOutOfBoundsException();
    } else {
      int cellIndex = this.cellIndex(index);
      int bitIndex = this.bitIndex(index, cellIndex);
      return (int) (this.data[cellIndex] >> bitIndex & this.maxValue);
    }
  }

  @Override
  public void write(Object bufObj, ProtocolVersion version) {
    Preconditions.checkArgument(bufObj instanceof ByteBuf);
    LimboProtocolUtils.writeLongArray((ByteBuf) bufObj, this.data);
  }

  @Override
  public int getBitsPerEntry() {
    return this.bitsPerEntry;
  }

  @Override
  public int getDataLength() {
    return ProtocolUtils.varIntBytes(this.data.length) + this.data.length * 8;
  }

  @Override
  public long[] getData() {
    return this.data;
  }

  @Override
  public CompactStorage copy() {
    return new BitStorage116(this.bitsPerEntry, this.size, Arrays.copyOf(this.data, this.data.length));
  }

  private int cellIndex(int index) {
    return (int) (index * this.divideMultiply + this.divideAdd >> 32 >> this.divideShift);
  }

  private int bitIndex(int index, int cellIndex) {
    return (index - cellIndex * this.valuesPerLong) * this.bitsPerEntry;
  }
}
