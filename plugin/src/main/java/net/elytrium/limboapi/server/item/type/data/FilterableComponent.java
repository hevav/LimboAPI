package net.elytrium.limboapi.server.item.type.data;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public record FilterableComponent(VersionLessComponentHolder raw, @Nullable VersionLessComponentHolder filtered) {

  public void write(ByteBuf buf, ProtocolVersion version) {
    this.raw.write(buf, version);
    LimboProtocolUtils.writeOptional(buf, this.filtered, filtered -> filtered.write(buf, version));
  }

  public static FilterableComponent read(ByteBuf buf, ProtocolVersion version) {
    return new FilterableComponent(VersionLessComponentHolder.read(buf, version), LimboProtocolUtils.readOptional(buf, () -> VersionLessComponentHolder.read(buf, version)));
  }
}
