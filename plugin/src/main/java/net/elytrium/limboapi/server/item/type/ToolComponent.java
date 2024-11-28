package net.elytrium.limboapi.server.item.type;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.item.AbstractItemComponent;
import net.elytrium.limboapi.server.item.type.data.HolderSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ToolComponent extends AbstractItemComponent<ToolComponent.Value> {

  @Override
  public void read(ByteBuf buf, ProtocolVersion version) {
    this.setValue(Value.read(buf));
  }

  @Override
  public void write(ByteBuf buf, ProtocolVersion version) {
    this.getValue().write(buf);
  }

  public record Value(Collection<Rule> rules, float defaultMiningSpeed, int damagePerBlock) {

    public void write(ByteBuf buf) {
      LimboProtocolUtils.writeCollection(buf, this.rules, Rule::write);
      buf.writeFloat(this.defaultMiningSpeed);
      ProtocolUtils.writeVarInt(buf, this.damagePerBlock);
    }

    public static Value read(ByteBuf buf) {
      return new Value(LimboProtocolUtils.readCollection(buf, Rule::read), buf.readFloat(), ProtocolUtils.readVarInt(buf));
    }

    public record Rule(HolderSet blocks, @Nullable Float speed, @Nullable Boolean correctForDrops) {

      public void write(ByteBuf buf) {
        this.blocks.write(buf);
        LimboProtocolUtils.writeOptional(buf, this.speed, buf::writeFloat);
        LimboProtocolUtils.writeOptional(buf, this.correctForDrops, buf::writeBoolean);
      }

      public static Rule read(ByteBuf buf) {
        return new Rule(HolderSet.read(buf), LimboProtocolUtils.readOptional(buf, buf::readFloat), LimboProtocolUtils.readOptional(buf, buf::readBoolean));
      }
    }
  }
}
