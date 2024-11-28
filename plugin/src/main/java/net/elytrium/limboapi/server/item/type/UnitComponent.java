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
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.item.AbstractItemComponent;

public class UnitComponent extends AbstractItemComponent<Object> {

  public static final UnitComponent INSTANCE = new UnitComponent();

  private UnitComponent() {

  }

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {

  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {

  }
}
