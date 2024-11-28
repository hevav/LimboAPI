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

package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class EnchantmentsComponent extends AbstractItemComponent<EnchantmentsComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(Map<Integer, Integer> enchantments, boolean showInTooltip) {

    public void write(ByteBuf buf) {
      LimboProtocolUtils.writeMap(buf, this.enchantments, ProtocolUtils::writeVarInt, ProtocolUtils::writeVarInt);
      buf.writeBoolean(this.showInTooltip);
    }

    public static Value read(ByteBuf buf) {
      return new Value(LimboProtocolUtils.readMap(buf, ProtocolUtils::readVarInt, ProtocolUtils::readVarInt), buf.readBoolean());
    }
  }
}
