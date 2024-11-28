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

package net.elytrium.limboapi.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.UnsignedPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.injection.login.confirmation.LoginConfirmHandler;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packets.c2s.MoveOnGroundOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePacket;
import net.elytrium.limboapi.protocol.packets.c2s.MovePositionOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.MoveRotationOnlyPacket;
import net.elytrium.limboapi.protocol.packets.c2s.ChatSessionUpdatePacket;
import net.elytrium.limboapi.protocol.packets.c2s.AcceptTeleportationPacket;
import net.elytrium.limboapi.utils.Reflection;

public class LimboSessionHandlerImpl implements MinecraftSessionHandler {

  public static final MethodHandle TEARDOWN_METHOD = Reflection.findVirtualVoid(ConnectedPlayer.class, "teardown");

  private final LimboAPI plugin;
  private final LimboImpl limbo;
  private final ConnectedPlayer player;
  private final LimboSessionHandler callback;
  private final StateRegistry originalState;
  private final MinecraftSessionHandler originalHandler;
  private final RegisteredServer previousServer;
  private final CompletableFuture<Object> playTransition = new CompletableFuture<>();
  private final CompletableFuture<Object> configTransition = new CompletableFuture<>();
  private final CompletableFuture<Object> chatSession = new CompletableFuture<>();

  private LimboPlayer limboPlayer;
  private ClientSettingsPacket settings;
  private String brand;
  private ScheduledFuture<?> keepAliveTask;
  private ScheduledFuture<?> chatSessionTimeoutTask;
  private ScheduledFuture<?> respawnTask;
  private long keepAliveKey;
  private boolean keepAlivePending;
  private int keepAlivesSkipped;
  private long keepAliveSentTime;
  private int ping = -1;
  private int cumulativeBytes;
  private boolean loaded;
  private boolean switching;
  private boolean disconnecting;
  private boolean joinGameTriggered;

  public LimboSessionHandlerImpl(LimboAPI plugin, LimboImpl limbo, ConnectedPlayer player,
      LimboSessionHandler callback, StateRegistry originalState, MinecraftSessionHandler originalHandler,
      RegisteredServer previousServer) {
    this.plugin = plugin;
    this.limbo = limbo;
    this.player = player;
    this.callback = callback;
    this.originalState = originalState;
    this.originalHandler = originalHandler;
    this.previousServer = previousServer;
    this.loaded = player.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_18_2);

    if (originalHandler instanceof LimboSessionHandlerImpl sessionHandler) {
      this.settings = sessionHandler.getSettings();
      this.brand = sessionHandler.getBrand();
      this.ping = sessionHandler.ping;
    }
  }

  @SuppressWarnings("UnnecessaryToStringCall")
  public void onConfig(LimboPlayer player) {
    this.loaded = true;
    this.limboPlayer = player;
    this.callback.onConfig(this.limbo, player);

    int serverReadTimeout = Objects.requireNonNullElseGet(this.limbo.getReadTimeout(), () -> this.plugin.getServer().getConfiguration().getReadTimeout());

    // We should always send multiple keepalives inside a single timeout to not trigger Netty read timeout
    serverReadTimeout /= 2;

    this.keepAliveTask = player.getScheduledExecutor().scheduleAtFixedRate(() -> {
      MinecraftConnection connection = this.player.getConnection();
      if (connection.isClosed()) {
        this.keepAliveTask.cancel(true);
        return;
      }

      if (this.keepAlivePending) {
        if (++this.keepAlivesSkipped == 2) {
          connection.closeWith(this.plugin.getPackets().getTimeOut(connection.getState()));
          if (Settings.IMP.MAIN.LOGGING_ENABLED) {
            LimboAPI.getLogger().warn("{} was kicked due to keepalive timeout", this.player.toString());
          }
        }
      } else if (this.keepAliveSentTime == 0 && this.originalHandler instanceof LimboSessionHandlerImpl sessionHandler) {
        this.keepAliveKey = sessionHandler.keepAliveKey;
        this.keepAlivePending = sessionHandler.keepAlivePending;
        this.keepAlivesSkipped = sessionHandler.keepAlivesSkipped;
        this.keepAliveSentTime = sessionHandler.keepAliveSentTime;
      } else {
        this.keepAliveKey = ThreadLocalRandom.current().nextInt();
        KeepAlivePacket keepAlive = new KeepAlivePacket();
        keepAlive.setRandomId(this.keepAliveKey);
        connection.write(keepAlive);
        this.keepAlivePending = true;
        this.keepAlivesSkipped = 0;
        this.keepAliveSentTime = System.currentTimeMillis();
      }
    }, this.originalHandler instanceof LimboSessionHandlerImpl ? 0 : 250, serverReadTimeout, TimeUnit.MILLISECONDS);
  }

  public void onSpawn() {
    this.callback.onSpawn(this.limbo, this.limboPlayer);

    // Player is spawned, so can trust that transition to the PLAY state is complete
    this.playTransition.complete(this);
  }

  public void disconnectToConfig(Runnable runnable) {
    if (this.configTransition.isDone()) {
      runnable.run();
      return;
    }

    this.release();

    this.switching = true;
    this.loaded = false;

    if (this.player.isOnlineMode() && this.player.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21_2) && this.joinGameTriggered) {
      // There is a race condition in the client then it reconnects too quickly (https://bugs.mojang.com/browse/MC-272506)
      if (!this.chatSession.isDone() && this.chatSessionTimeoutTask == null) {
        this.chatSessionTimeoutTask = this.player.getConnection().eventLoop()
            .schedule(() -> this.chatSession.complete(this), Settings.IMP.MAIN.CHAT_SESSION_PACKET_TIMEOUT, TimeUnit.MILLISECONDS);
      }
      this.chatSession.thenRunAsync(() -> {
        this.player.getConnection().write(StartUpdatePacket.INSTANCE);
        this.configTransition.thenRun(this::disconnected).thenRun(runnable);
      }, this.player.getConnection().eventLoop());
    } else {
      this.player.getConnection().write(StartUpdatePacket.INSTANCE);
      this.configTransition.thenRun(this::disconnected).thenRun(runnable);
    }
  }

  @Override
  @SuppressWarnings("UnnecessaryToStringCall")
  public boolean handle(FinishedUpdatePacket packet) {
    // Switching to CONFIG state
    if (this.player.getConnection().getState() != StateRegistry.CONFIG) {
      this.plugin.setActiveSessionHandler(this.player.getConnection(), StateRegistry.CONFIG, this);

      if (!this.loaded && !this.disconnecting) {
        this.limbo.spawnPlayerLocal(this.callback.getClass(), this, this.player, this.player.getConnection());
      } else if (this.switching) {
        this.switching = false;
        this.configTransition.complete(this);
      } else {
        this.player.getConnection().closeWith(this.plugin.getPackets().getInvalidSwitch());

        if (Settings.IMP.MAIN.LOGGING_ENABLED) {
          LimboAPI.getLogger().warn("{} sent an unexpected state switch confirmation", this.player.toString());
        }
      }

      return true;
    }

    this.plugin.setState(this.player.getConnection(), this.limbo.localStateRegistry);

    this.limbo.onSpawn(this.callback.getClass(), this.player.getConnection(), this.player, this);
    this.player.getConnection().flush();
    return true;
  }

  public boolean handle(MovePacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      double posX = packet.getX();
      double posY = packet.getY();
      double posZ = packet.getZ();
      float yaw = packet.getYaw();
      float pitch = packet.getPitch();
      this.callback.onMove(posX, posY, posZ, yaw, pitch);
      this.callback.onMove(posX, posY, posZ);
      this.callback.onRotate(yaw, pitch);
    }

    return true;
  }

  public boolean handle(MovePositionOnlyPacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onMove(packet.getX(), packet.getY(), packet.getZ());
    }

    return true;
  }

  public boolean handle(MoveRotationOnlyPacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
      this.callback.onRotate(packet.getYaw(), packet.getPitch());
    }

    return true;
  }

  public boolean handle(MoveOnGroundOnlyPacket packet) {
    if (this.loaded) {
      this.callback.onGround(packet.isOnGround());
    }

    return true;
  }

  public boolean handle(AcceptTeleportationPacket packet) {
    if (this.loaded) {
      this.callback.onTeleport(packet.getId());
    }

    return true;
  }

  @Override
  @SuppressWarnings("UnnecessaryToStringCall")
  public boolean handle(KeepAlivePacket packet) {
    MinecraftConnection connection = this.player.getConnection();
    if (this.keepAlivePending) {
      if (packet.getRandomId() != this.keepAliveKey) {
        connection.closeWith(this.plugin.getPackets().getInvalidPing());
        if (Settings.IMP.MAIN.LOGGING_ENABLED) {
          LimboAPI.getLogger().warn("{} sent an invalid keepalive", this.player.toString());
        }

        return false;
      } else {
        this.keepAlivePending = false;
        this.keepAlivesSkipped = 0;
        int currentPing = (int) (System.currentTimeMillis() - this.keepAliveSentTime);
        this.ping = this.ping == -1 ? currentPing : (this.ping * 3 + currentPing) / 4;
        return true;
      }
    } else {
      connection.closeWith(this.plugin.getPackets().getInvalidPing());
      if (Settings.IMP.MAIN.LOGGING_ENABLED) {
        LimboAPI.getLogger().warn("{} sent an unexpected keepalive", this.player.toString());
      }

      return false;
    }
  }

  @Override
  public boolean handle(LegacyChatPacket packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(KeyedPlayerChatPacket packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(KeyedPlayerCommandPacket packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  @Override
  public boolean handle(SessionPlayerChatPacket packet) {
    return this.handleChat(packet.getMessage());
  }

  @Override
  public boolean handle(SessionPlayerCommandPacket packet) {
    return this.handleChat("/" + packet.getCommand());
  }

  private boolean handleChat(String message) {
    int messageLength = message.length();
    if (messageLength > Settings.IMP.MAIN.MAX_CHAT_MESSAGE_LENGTH) {
      this.kickTooBigPacket("chat", messageLength);
    } else {
      this.callback.onChat(message);
    }

    return true;
  }

  @Override
  public void handleUnknown(ByteBuf packet) {
    int readableBytes = packet.readableBytes();
    this.cumulativeBytes += readableBytes;
    if (readableBytes > Settings.IMP.MAIN.MAX_UNKNOWN_PACKET_LENGTH) {
      this.kickTooBigPacket("unknown", readableBytes);
    } else if (this.cumulativeBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
      this.kickTooBigPacket("unknown, multi", this.cumulativeBytes);
    }
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof ClientSettingsPacket clientSettings) {
      this.settings = clientSettings;
    } else if (packet instanceof ChatSessionUpdatePacket) {
      if (this.chatSessionTimeoutTask != null) {
        this.chatSessionTimeoutTask.cancel(true);
      }
      this.chatSession.complete(this);
    } else if (packet instanceof PluginMessagePacket pluginMessage) {
      int singleLength = pluginMessage.content().readableBytes() + pluginMessage.getChannel().length() * 4;
      this.cumulativeBytes += singleLength;
      if (singleLength > Settings.IMP.MAIN.MAX_SINGLE_GENERIC_PACKET_LENGTH) {
        this.kickTooBigPacket("generic (PluginMessage packet (custom payload)), single", singleLength);
        return;
      } else if (this.cumulativeBytes > Settings.IMP.MAIN.MAX_MULTI_GENERIC_PACKET_LENGTH) {
        this.kickTooBigPacket("generic (PluginMessage packet (custom payload)), multi", this.cumulativeBytes);
        return;
      }

      if (this.player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_2) && PluginMessageUtil.isMcBrand(pluginMessage)) {
        try {
          this.brand = ProtocolUtils.readString(pluginMessage.content().slice(), Settings.IMP.MAIN.MAX_BRAND_NAME_LENGTH);
        } catch (QuietDecoderException ignored) {
          this.kickTooBigPacket("brand name", pluginMessage.content().readableBytes());
          return;
        }
      }
    } else if (packet instanceof UnsignedPlayerCommandPacket commandPacket) {
      this.handleChat("/" + commandPacket.getCommand());
      return;
    }

    this.callback.onGeneric(packet);
  }

  @SuppressWarnings("UnnecessaryToStringCall")
  private void kickTooBigPacket(String type, int length) {
    this.player.getConnection().closeWith(this.plugin.getPackets().getTooBigPacket());
    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      LimboAPI.getLogger().warn("{} sent too big packet (type: {}, length: {})", this.player.toString(), type, length);
    }
  }

  public void release() {
    if (this.keepAliveTask != null) {
      this.keepAliveTask.cancel(true);
    }

    if (this.respawnTask != null) {
      this.respawnTask.cancel(true);
    }

    if (this.loaded) {
      this.limbo.onDisconnect();
      this.callback.onDisconnect();
    }
  }

  @Override
  public void disconnected() {
    //this.disconnected = true;
    this.release();

    if (Settings.IMP.MAIN.LOGGING_ENABLED) {
      LimboAPI.getLogger().info("{} has disconnected from the {} Limbo", this.player.toString(), this.limbo.getName());
    }

    MinecraftConnection connection = this.player.getConnection();
    if (connection.isClosed()) {
      try {
        TEARDOWN_METHOD.invokeExact(this.player);
      } catch (Throwable t) {
        throw new ReflectionException(t);
      }

      return;
    }

    if (!(this.originalHandler instanceof AuthSessionHandler)
        && !(this.originalHandler instanceof LimboSessionHandlerImpl)
        && !(this.originalHandler instanceof ClientPlaySessionHandler) // cause issues with server switching
        && !(this.originalHandler instanceof LoginConfirmHandler)) {
      connection.eventLoop().execute(() -> {
        // Ensure that originalHandler is returned to the proper state
        if (connection.getState() != this.originalState) {
          connection.addSessionHandler(this.originalState, this.originalHandler);
        } else {
          this.plugin.setActiveSessionHandler(connection, connection.getState(), this.originalHandler);
        }
      });
    }

    ChannelPipeline pipeline = connection.getChannel().pipeline();

    if (pipeline.get(LimboProtocol.READ_TIMEOUT) != null) {
      pipeline.replace(LimboProtocol.READ_TIMEOUT, Connections.READ_TIMEOUT, new ReadTimeoutHandler(this.plugin.getServer().getConfiguration().getReadTimeout(), TimeUnit.MILLISECONDS));
    }
  }

  public void disconnect(Runnable runnable) {
    if (!this.disconnecting) {
      this.disconnecting = true;
      if (this.player.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        runnable.run();
      } else {
        this.playTransition.thenRun(runnable);
      }
    }
  }

  public RegisteredServer getPreviousServer() {
    return this.previousServer;
  }

  public int getPing() {
    return this.ping;
  }

  public void setJoinGameTriggered(boolean joinGameTriggered) {
    this.joinGameTriggered = joinGameTriggered;
  }

  public void setRespawnTask(ScheduledFuture<?> respawnTask) {
    if (this.respawnTask != null) {
      this.respawnTask.cancel(true);
    }

    this.respawnTask = respawnTask;
  }

  public ClientSettingsPacket getSettings() {
    return this.settings;
  }

  public String getBrand() {
    return this.brand;
  }
}
