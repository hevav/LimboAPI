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
 *
 * This file contains some parts of Velocity, licensed under the AGPLv3 License (AGPLv3).
 *
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.injection.login;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.VelocityConnectionEvent;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressorAndLengthEncoder;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccessPacket;
import com.velocitypowered.proxy.protocol.packet.SetCompressionPacket;
import com.velocitypowered.proxy.tablist.InternalTabList;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import java.lang.invoke.MethodHandle;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.UUID;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.injection.dummy.ClosedMinecraftConnection;
import net.elytrium.limboapi.injection.login.confirmation.LoginConfirmHandler;
import net.elytrium.limboapi.injection.packet.ServerLoginSuccessHook;
import net.elytrium.limboapi.injection.tablist.RewritingKeyedVelocityTabList;
import net.elytrium.limboapi.injection.tablist.RewritingVelocityTabList;
import net.elytrium.limboapi.injection.tablist.RewritingVelocityTabListLegacy;
import net.elytrium.limboapi.utils.Reflection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LoginListener {

  private static final MethodHandle DELEGATE_FIELD = Reflection.findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);
  static final MethodHandle MC_CONNECTION_SETTER = Reflection.findSetter(AuthSessionHandler.class, "mcConnection", MinecraftConnection.class);
  private static final MethodHandle CONNECTED_PLAYER_CONSTRUCTOR = Reflection.findConstructor(
      ConnectedPlayer.class,
      VelocityServer.class, GameProfile.class, MinecraftConnection.class, InetSocketAddress.class, String.class, boolean.class, IdentifiedKey.class
  );
  private static final MethodHandle TAB_LIST_SETTER = Reflection.findSetter(ConnectedPlayer.class, "tabList", InternalTabList.class);
  private static final MethodHandle SPAWNED_FIELD = Reflection.findSetter(ClientPlaySessionHandler.class, "spawned", boolean.class);

  private final LimboAPI plugin;
  private final VelocityServer server;

  public LoginListener(LimboAPI plugin, VelocityServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Subscribe
  public void hookInitialServer(PlayerChooseInitialServerEvent event) {
    Player player = event.getPlayer();
    if (this.plugin.hasNextServer(player)) {
      event.setInitialServer(this.plugin.getNextServer(player));
    }

    this.plugin.setLimboJoined(player);
  }

  @SuppressWarnings("ConstantConditions")
  public void hookLoginSession(GameProfileRequestEvent event) throws Throwable {
    LoginInboundConnection inboundConnection = (LoginInboundConnection) event.getConnection();
    // In some cases, e.g., if the player logged out or was kicked right before the GameProfileRequestEvent hook,
    // the connection will be broken (possibly by GC) and we can't get it from the delegate field.
    if (LoginInboundConnection.class.isAssignableFrom(inboundConnection.getClass())) {
      // Changing mcConnection to the closed one. For what? To break the "initializePlayer"
      // method (which checks mcConnection.isActive()) and to override it. :)
      InitialInboundConnection inbound = (InitialInboundConnection) DELEGATE_FIELD.invokeExact(inboundConnection);
      MinecraftConnection connection = inbound.getConnection();

      // Ensure that this method is always invoked inside EventLoop.
      if (!connection.eventLoop().inEventLoop()) {
        connection.eventLoop().execute(() -> {
          try {
            this.hookLoginSession(event);
          } catch (Throwable t) {
            throw new IllegalStateException("failed to handle login request", t);
          }
        });
        return;
      }

      AuthSessionHandler handler = (AuthSessionHandler) connection.getActiveSessionHandler();
      MC_CONNECTION_SETTER.invokeExact(handler, ClosedMinecraftConnection.INSTANCE);

      ProtocolVersion version = connection.getProtocolVersion();
      LoginConfirmHandler loginHandler = null;
      boolean v1_20_2 = version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2);
      if (v1_20_2) {
        connection.setActiveSessionHandler(StateRegistry.LOGIN, loginHandler = new LoginConfirmHandler(this.plugin, connection));
      }

      // From Velocity
      if (!connection.isClosed()) {
        try {
          IdentifiedKey playerKey = inboundConnection.getIdentifiedKey();
          if (playerKey != null) {
            if (playerKey.getSignatureHolder() == null) {
              if (playerKey instanceof IdentifiedKeyImpl unlinkedKey) {
                // Failsafe
                if (!unlinkedKey.internalAddHolder(event.getGameProfile().getId())) {
                  playerKey = null;
                }
              }
            } else if (!Objects.equals(playerKey.getSignatureHolder(), event.getGameProfile().getId())) {
              playerKey = null;
            }
          }

          // Initiate a regular connection and move over to it.
          ConnectedPlayer player = (ConnectedPlayer) CONNECTED_PLAYER_CONSTRUCTOR.invokeExact(
              this.server,
              event.getGameProfile(),
              connection,
              inboundConnection.getVirtualHost().orElse(null),
              inboundConnection.getRawVirtualHost().orElse(null),
              event.isOnlineMode(),
              playerKey
          );

          TAB_LIST_SETTER.invokeExact(player, (InternalTabList) (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3)
              ? new RewritingVelocityTabList(player)
              : version.noLessThan(ProtocolVersion.MINECRAFT_1_8)
                  ? new RewritingKeyedVelocityTabList(player, this.server)
                  : new RewritingVelocityTabListLegacy(player, this.server)
          ));

          if (v1_20_2) {
            loginHandler.setPlayer(player);
          }

          if (this.server.canRegisterConnection(player)) {
            if (!connection.isClosed()) {
              // Complete the Login process
              var configuration = this.server.getConfiguration();
              int threshold = configuration.getCompressionThreshold();
              ChannelPipeline pipeline = connection.getChannel().pipeline();
              boolean compressionEnabled = threshold >= 0 && version.noLessThan(ProtocolVersion.MINECRAFT_1_8);
              if (compressionEnabled) {
                connection.write(new SetCompressionPacket(threshold));
                this.plugin.fixDecompressor(pipeline, threshold, true);
                if (Settings.IMP.MAIN.COMPATIBILITY_MODE) {
                  pipeline.addBefore(Connections.MINECRAFT_ENCODER, Connections.COMPRESSION_ENCODER, new MinecraftCompressorAndLengthEncoder(threshold, Natives.compress.get().create(configuration.getCompressionLevel())));
                  pipeline.remove(Connections.FRAME_ENCODER);
                } else {
                  pipeline.addFirst(Connections.COMPRESSION_ENCODER, new ChannelOutboundHandlerAdapter());
                }
              }

              if (!Settings.IMP.MAIN.COMPATIBILITY_MODE) {
                pipeline.remove(Connections.FRAME_ENCODER);
              }

              this.plugin.inject3rdParty(player, connection, pipeline);
              if (compressionEnabled) {
                pipeline.fireUserEventTriggered(VelocityConnectionEvent.COMPRESSION_ENABLED);
              } else {
                pipeline.fireUserEventTriggered(VelocityConnectionEvent.COMPRESSION_DISABLED);
              }

              UUID playerUniqueId = player.getUniqueId();
              if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
                playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
              }

              ServerLoginSuccessPacket success = new ServerLoginSuccessPacket();
              success.setUsername(player.getUsername());
              success.setProperties(player.getGameProfileProperties());
              success.setUuid(playerUniqueId);

              if (Settings.IMP.MAIN.COMPATIBILITY_MODE) {
                connection.write(success);
              } else {
                ServerLoginSuccessHook successHook = new ServerLoginSuccessHook();
                successHook.setUsername(player.getUsername());
                successHook.setProperties(player.getGameProfileProperties());
                successHook.setUuid(playerUniqueId);
                connection.write(successHook);

                ChannelHandler compressionHandler = pipeline.get(Connections.COMPRESSION_ENCODER);
                if (compressionHandler == null) {
                  ChannelHandler frameHandler = pipeline.get(Connections.FRAME_ENCODER);
                  if (frameHandler != null) {
                    pipeline.remove(frameHandler);
                  }

                  connection.write(this.plugin.encodeSingleLoginUncompressed(success, version));
                } else {
                  connection.write(this.plugin.encodeSingleLogin(success, version));
                }
              }

              LimboAPI.setClientUniqueId(player, playerUniqueId);

              if (v1_20_2) {
                loginHandler.thenRun(() -> this.fireRegisterEvent(player, connection, inbound, handler));
              } else {
                connection.setState(StateRegistry.PLAY);
                this.fireRegisterEvent(player, connection, inbound, handler);
              }
            }
          } else {
            player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
          }
        } catch (Throwable t) {
          throw new ReflectionException(t);
        }
      }
    }
  }

  private void fireRegisterEvent(ConnectedPlayer player, MinecraftConnection connection, InitialInboundConnection inbound, AuthSessionHandler handler) {
    this.server.getEventManager().fire(new LoginLimboRegisterEvent(player)).thenAcceptAsync(limboRegisterEvent -> {
      LoginTasksQueue queue = new LoginTasksQueue(this.plugin, handler, this.server, player, inbound, limboRegisterEvent.getOnJoinCallbacks());
      this.plugin.addLoginQueue(player, queue);
      this.plugin.setKickCallback(player, limboRegisterEvent.getOnKickCallback());
      queue.next();
    }, connection.eventLoop()).exceptionally(t -> {
      LimboAPI.getLogger().error("Exception while registering LimboAPI login handlers for {}", player.toString(), t);
      return null;
    });
  }

  @Subscribe
  public void hookPlaySession(ServerConnectedEvent event) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    MinecraftConnection connection = player.getConnection();

    // 1.20.2+ can ignore this, as it should be despawned by default
    if (connection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      return;
    }

    connection.eventLoop().execute(() -> {
      if (!(connection.getActiveSessionHandler() instanceof ClientPlaySessionHandler)) {
        try {
          ClientPlaySessionHandler playHandler = new ClientPlaySessionHandler(this.server, player);
          SPAWNED_FIELD.invokeExact(playHandler, this.plugin.isLimboJoined(player));
          connection.setActiveSessionHandler(connection.getState(), playHandler);
        } catch (Throwable t) {
          throw new ReflectionException(t);
        }
      }
    });
  }
}
