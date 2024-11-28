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

package net.elytrium.limboapi.injection.login.confirmation;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledgedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;

public class LoginConfirmHandler implements MinecraftSessionHandler {

  private final LimboAPI plugin;
  private final CompletableFuture<Object> confirmation = new CompletableFuture<>();
  private final List<MinecraftPacket> queuedPackets = new ArrayList<>();
  private final MinecraftConnection connection;
  private ConnectedPlayer player;

  public LoginConfirmHandler(LimboAPI plugin, MinecraftConnection connection) {
    this.plugin = plugin;
    this.connection = connection;
  }

  public void setPlayer(ConnectedPlayer player) {
    this.player = player;
  }

  public boolean isDone() {
    return this.confirmation.isDone();
  }

  public CompletableFuture<Void> thenRun(Runnable runnable) {
    return this.confirmation.thenRun(runnable);
  }

  @SuppressWarnings("UnnecessaryCallToStringValueOf")
  public void waitForConfirmation(Runnable runnable) {
    this.thenRun(() -> {
      try {
        runnable.run();
      } catch (Throwable t) {
        LimboAPI.getLogger().error("Failed to confirm transition for {}", this.player.toString(), t);
      }

      try {
        ChannelHandlerContext ctx = this.connection.getChannel().pipeline().context(this.connection);
        for (MinecraftPacket packet : this.queuedPackets) {
          try {
            this.connection.channelRead(ctx, packet);
          } catch (Throwable t) {
            LimboAPI.getLogger().error("{}: failed to handle packet {} for handler {}", this.player.toString(), packet.toString(), String.valueOf(this.connection.getActiveSessionHandler()), t);
          }
        }

        this.queuedPackets.clear();
      } catch (Throwable t) {
        LimboAPI.getLogger().error("Failed to process packet queue for {}", this.player.toString(), t);
      }
    });
  }

  @Override
  public boolean handle(LoginAcknowledgedPacket packet) {
    this.plugin.setState(this.connection, StateRegistry.CONFIG);
    this.confirmation.complete(this);
    return true;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    // As Velocity/LimboAPI can easily skip packets due to random delays, packets should be queued
    if (this.connection.getState() == StateRegistry.CONFIG) {
      this.queuedPackets.add(ReferenceCountUtil.retain(packet));
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    this.connection.close(true);
  }

  @Override
  public void disconnected() {
    try {
      if (this.player != null) {
        try {
          LimboSessionHandlerImpl.TEARDOWN_METHOD.invokeExact(this.player);
        } catch (Throwable t) {
          throw new ReflectionException(t);
        }
      }
    } finally {
      for (MinecraftPacket packet : this.queuedPackets) {
        ReferenceCountUtil.release(packet);
      }
    }
  }
}
