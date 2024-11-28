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
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.kyori.adventure.text.Component;

public class TextComponent extends AbstractItemComponent<Component> { // TODO VersionLessComponentHolder

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(ComponentHolder.read(buf, version).getComponent());
    //this.setValue(VersionLessComponentHolder.read(buf, version));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    new ComponentHolder(version, this.getValue()).write(buf);
    //this.getValue().write(buf, version);
  }
  /*

  static Object readComponent(ByteBuf buf, ProtocolVersion version) {
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3) ? ProtocolUtils.readBinaryTag(buf, version, BinaryTagIO.reader()) : ProtocolUtils.readString(buf);
  }

  static void writeComponent(ByteBuf buf, ProtocolVersion version, Object value) {
    if (value instanceof ComponentHolder holder) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
        ProtocolUtils.writeBinaryTag(buf, version, holder.getBinaryTag());
      } else {
        ProtocolUtils.writeString(buf, holder.getJson());
      }
    } else if (value instanceof Component component) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
        ProtocolUtils.writeBinaryTag(buf, version, ComponentHolder.serialize(GsonComponentSerializer.gson().serializeToTree(component)));
      } else {
        ProtocolUtils.writeString(buf, ProtocolUtils.getJsonChatSerializer(version).serialize(component));
      }
    } else if (value instanceof BinaryTag tag) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
        ProtocolUtils.writeBinaryTag(buf, version, tag);
      } else {
        ProtocolUtils.writeString(buf, ComponentHolder.deserialize(tag).toString());
      }
    } else if (value instanceof String string) {
      if (string.charAt(0) != '{') {
        throw new IllegalArgumentException("Json string expected");
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
        ProtocolUtils.writeBinaryTag(buf, version, ComponentHolder.serialize(GsonComponentSerializer.gson().serializeToTree(ProtocolUtils.getJsonChatSerializer(version).deserialize(string))));
      } else {
        ProtocolUtils.writeString(buf, string);
      }
    }
  }
  */
}
