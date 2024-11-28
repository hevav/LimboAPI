package net.elytrium.limboapi.protocol.packets.s2c;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.regex.Pattern;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public record UpdateSignPacket(int posX, int posY, int posZ, Component[] lines) implements MinecraftPacket {

  public UpdateSignPacket(VirtualBlockEntity.Entry entry) {
    this(entry.getPosX(), entry.getPosY(), entry.getPosZ(), UpdateSignPacket.extractLines(entry.getNbt(ProtocolVersion.MINECRAFT_1_9_4)));
  }

  // TODO get rid of this method when JEP 447
  private static Component[] extractLines(CompoundBinaryTag nbt) {
    Component[] lines = new Component[4];
    var serializer = ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_9_4);
    for (int i = 0; i < 4; i++) {
      lines[i] = serializer.deserialize(nbt.getString("Text" + (i + 1)));
    }

    return lines;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    boolean v1_7 = protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);
    if (v1_7) {
      buf.writeInt(this.posX);
      buf.writeShort(this.posY);
      buf.writeInt(this.posZ);
    } else {
      LimboProtocolUtils.writeBlockPos(buf, protocolVersion, this.posX, this.posY, this.posZ);
    }
    var serializer = protocolVersion.noGreaterThan(ProtocolVersion.MINECRAFT_1_8) ? LegacyComponentSerializer.legacySection() : ProtocolUtils.getJsonChatSerializer(protocolVersion);
    for (int i = 0; i < 4; ++i) {
      String line = serializer.serialize(this.lines[i]);
      if (v1_7) {
        // https://github.com/ViaVersion/ViaRewind/blob/4.0.3/common/src/main/java/com/viaversion/viarewind/protocol/v1_8to1_7_6_10/rewriter/WorldPacketRewriter1_8.java#L175
        if (line.startsWith("§f")) {
          line = line.substring(2);
        }

        line = LegacyUtil.removeUnusedColor(line);
        if (line.length() > 15) {
          line = PlainTextComponentSerializer.plainText().serialize(this.lines[i]);
          if (line.length() > 15) {
            line = line.substring(0, 15);
          }
        }
      }

      ProtocolUtils.writeString(buf, line);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    throw new IllegalStateException();
  }

  // https://github.com/ViaVersion/ViaRewind/blob/4.0.3/common/src/main/java/com/viaversion/viarewind/utils/ChatUtil.java#L80
  private static class LegacyUtil {

    private static final Pattern UNUSED_COLOR_PATTERN = Pattern.compile("(?>(?>§[0-fk-or])*(§r|\\Z))|(?>(?>§[0-f])*(§[0-f]))");

    private static String removeUnusedColor(String legacy) {
      legacy = LegacyUtil.UNUSED_COLOR_PATTERN.matcher(legacy).replaceAll("$1$2");
      StringBuilder builder = new StringBuilder();
      char last = '0';
      for (int i = 0; i < legacy.length(); ++i) {
        char current = legacy.charAt(i);
        if (current != '§' || i == legacy.length() - 1) {
          builder.append(current);
          continue;
        }

        current = legacy.charAt(++i);
        if (current == last) {
          continue;
        }

        builder.append('§').append(current);
        last = current;
      }

      return builder.toString();
    }
  }
}
