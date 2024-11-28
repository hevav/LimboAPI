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
 *
 * This file contains some parts of Velocity, licensed under the AGPLv3 License (AGPLv3).
 *
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.injection.packet;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import net.elytrium.limboapi.Settings;

public class MinecraftLimitedCompressDecoder extends MinecraftCompressDecoder implements LimboCompressDecoder {

  private final int threshold;
  private final VelocityCompressor compressor;

  private int uncompressedCap = Settings.IMP.MAIN.MAX_PACKET_LENGTH_TO_SUPPRESS_IT;

  public MinecraftLimitedCompressDecoder(int threshold, VelocityCompressor compressor) {
    super(threshold, compressor);
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    int claimedUncompressedSize = ProtocolUtils.readVarInt(in);
    if (claimedUncompressedSize == 0) {
      out.add(in.retain());
    } else if (claimedUncompressedSize > Settings.IMP.MAIN.MAX_SINGLE_GENERIC_PACKET_LENGTH) {
      ctx.close();
    } else if (claimedUncompressedSize >= this.threshold && claimedUncompressedSize <= this.uncompressedCap) {
      var alloc = ctx.alloc();
      ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(alloc, this.compressor, in);
      ByteBuf uncompressed = MoreByteBufUtils.preferredBuffer(alloc, this.compressor, claimedUncompressedSize);
      try {
        this.compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
        out.add(uncompressed);
      } catch (Exception e) {
        uncompressed.release();
        throw e;
      } finally {
        compatibleIn.release();
      }
    }
  }

  public void setUncompressedCap(int uncompressedCap) {
    this.uncompressedCap = uncompressedCap;
  }
}
