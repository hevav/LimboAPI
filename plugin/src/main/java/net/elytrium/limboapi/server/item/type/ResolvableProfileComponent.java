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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ResolvableProfileComponent extends AbstractItemComponent<ResolvableProfileComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(@Nullable String name, @Nullable UUID id, List<GameProfile.Property> properties) {

    public void write(ByteBuf buf) {
      LimboProtocolUtils.writeOptional(buf, this.name, ProtocolUtils::writeString);
      LimboProtocolUtils.writeOptional(buf, this.id, ProtocolUtils::writeUuid);
      ProtocolUtils.writeProperties(buf, this.properties);
    }

    public static Value read(ByteBuf buf) {
      return new Value(
          LimboProtocolUtils.readOptional(buf, () -> ProtocolUtils.readString(buf, 16)),
          LimboProtocolUtils.readOptional(buf, ProtocolUtils::readUuid),
          ProtocolUtils.readProperties(buf)
      );
    }
  }
}
