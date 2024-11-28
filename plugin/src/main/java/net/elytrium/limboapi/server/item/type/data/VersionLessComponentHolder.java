package net.elytrium.limboapi.server.item.type.data;

import com.google.gson.JsonElement;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class VersionLessComponentHolder {

  private final Component component;
  private final String json;
  private final BinaryTag binaryTag;

  public VersionLessComponentHolder(Component component) {
    this.component = component;
    this.json = null;
    this.binaryTag = null;
  }

  public VersionLessComponentHolder(String json) {
    this.component = null;
    this.json = json;
    this.binaryTag = null;
  }

  public VersionLessComponentHolder(BinaryTag binaryTag) {
    this.component = null;
    this.json = null;
    this.binaryTag = binaryTag;
  }

  public Component getComponent(ProtocolVersion version) {
    if (this.component == null) {
      if (this.json != null) {
        return ProtocolUtils.getJsonChatSerializer(version).deserialize(this.json);
      } else if (this.binaryTag != null) {
        // TODO: replace this with adventure-text-serializer-nbt when velocity will
        JsonElement json = ComponentHolder.deserialize(this.binaryTag);
        try {
          return ProtocolUtils.getJsonChatSerializer(version).deserializeFromTree(json);
        } catch (Exception e) {
          LimboAPI.getLogger().error("Error converting binary component to JSON component! Binary: {} JSON: {}", this.binaryTag, json, e);
          throw e;
        }
      }
    }

    return this.component;
  }

  public String getJson(ProtocolVersion version) {
    if (this.json == null) {
      return ProtocolUtils.getJsonChatSerializer(version).serialize(this.getComponent(version));
    }

    return this.json;
  }

  public BinaryTag getBinaryTag(ProtocolVersion version) {
    if (this.binaryTag == null) {
      // TODO: replace this with adventure-text-serializer-nbt when velocity will
      return ComponentHolder.serialize(GsonComponentSerializer.gson().serializeToTree(this.getComponent(version)));
    }

    return this.binaryTag;
  }

  public void write(ByteBuf buf, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      ProtocolUtils.writeBinaryTag(buf, version, this.getBinaryTag(version));
    } else {
      ProtocolUtils.writeString(buf, this.getJson(version));
    }
  }

  public static VersionLessComponentHolder read(ByteBuf buf, ProtocolVersion version) {
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)
        ? new VersionLessComponentHolder(ProtocolUtils.readBinaryTag(buf, version, null))
        : new VersionLessComponentHolder(ProtocolUtils.readString(buf));
  }
}
