package net.elytrium.limboapi.server.item.type.data;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public record MobEffect(int effect, Details details) {

  public void write(ByteBuf buf) {
    ProtocolUtils.writeVarInt(buf, this.effect);
    this.details.write(buf);
  }

  public static MobEffect read(ByteBuf buf) {
    return new MobEffect(ProtocolUtils.readVarInt(buf), Details.read(buf));
  }

  public record Details(int amplifier, int duration, boolean ambient, boolean visible, boolean showIcon, @Nullable Details hiddenEffect) {

    public void write(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, this.amplifier);
      ProtocolUtils.writeVarInt(buf, this.duration);
      buf.writeBoolean(this.ambient);
      buf.writeBoolean(this.visible);
      buf.writeBoolean(this.showIcon);
      LimboProtocolUtils.writeOptional(buf, this.hiddenEffect, Details::write);
    }

    public static Details read(ByteBuf buf) {
      return new Details(
          ProtocolUtils.readVarInt(buf),
          ProtocolUtils.readVarInt(buf),
          buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
          LimboProtocolUtils.readOptional(buf, Details::read)
      );
    }
  }
}
