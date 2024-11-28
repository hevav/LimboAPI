package net.elytrium.limboapi.server.item.type.data;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public record FilterableString(String raw, @Nullable String filtered) {

  public void write(ByteBuf buf) {
    ProtocolUtils.writeString(buf, this.raw);
    LimboProtocolUtils.writeOptional(buf, this.filtered, ProtocolUtils::writeString);
  }

  public static FilterableString read(ByteBuf buf) {
    return new FilterableString(ProtocolUtils.readString(buf, 1024), LimboProtocolUtils.readOptional(buf, () -> ProtocolUtils.readString(buf, 1024)));
  }
}
