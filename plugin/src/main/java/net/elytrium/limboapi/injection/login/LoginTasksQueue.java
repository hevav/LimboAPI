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

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientConfigSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.injection.login.confirmation.LoginConfirmHandler;
import net.elytrium.limboapi.server.LimboSessionHandlerImpl;
import net.elytrium.limboapi.utils.LambdaUtil;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class LoginTasksQueue {

  private static final MethodHandle PROFILE_FIELD;
  private static final PermissionProvider DEFAULT_PERMISSIONS;
  private static final MethodHandle SET_PERMISSION_FUNCTION_METHOD;
  private static final MethodHandle INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR;
  private static final BiConsumer<Object, MinecraftConnection> MC_CONNECTION_SETTER;
  private static final MethodHandle CONNECT_TO_INITIAL_SERVER_METHOD;
  private static final MethodHandle SET_CLIENT_BRAND;
  public static final BiConsumer<ClientConfigSessionHandler, String> BRAND_CHANNEL_SETTER;

  private final LimboAPI plugin;
  private final Object handler;
  private final VelocityServer server;
  private final ConnectedPlayer player;
  private final InboundConnection inbound;
  private final Queue<Runnable> queue;

  public LoginTasksQueue(LimboAPI plugin, Object handler, VelocityServer server, ConnectedPlayer player,
                         InboundConnection inbound, Queue<Runnable> queue) {
    this.plugin = plugin;
    this.handler = handler;
    this.server = server;
    this.player = player;
    this.inbound = inbound;
    this.queue = queue;
  }

  public void next() {
    MinecraftConnection connection = this.player.getConnection();
    if (connection.getChannel().isActive()) {
      EventLoop eventLoop = connection.eventLoop();
      if (this.queue.isEmpty()) {
        eventLoop.execute(this::finish);
      } else {
        eventLoop.execute(Objects.requireNonNull(this.queue.poll()));
      }
    }
  }

  private void finish() {
    this.plugin.removeLoginQueue(this.player);

    EventManager eventManager = this.server.getEventManager();
    MinecraftConnection connection = this.player.getConnection();
    Logger logger = LimboAPI.getLogger();

    this.plugin.getEventManagerHook().proceedProfile(this.player.getGameProfile());
    eventManager.fire(new GameProfileRequestEvent(this.inbound, this.player.getGameProfile(), this.player.isOnlineMode())).thenAcceptAsync(
        gameProfile -> {
          try {
            UUID uuid = this.plugin.getInitialID(this.player);
            if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_1) <= 0) {
              connection.delayedWrite(new LegacyPlayerListItemPacket(
                  LegacyPlayerListItemPacket.REMOVE_PLAYER,
                  List.of(new LegacyPlayerListItemPacket.Item(uuid))
              ));

              connection.delayedWrite(new LegacyPlayerListItemPacket(
                  LegacyPlayerListItemPacket.ADD_PLAYER,
                  List.of(
                      new LegacyPlayerListItemPacket.Item(uuid)
                          .setName(gameProfile.getUsername())
                          .setProperties(gameProfile.getGameProfile().getProperties())
                  )
              ));
            } else if (connection.getState() != StateRegistry.CONFIG) {
              UpsertPlayerInfoPacket.Entry playerInfoEntry = new UpsertPlayerInfoPacket.Entry(uuid);
              playerInfoEntry.setDisplayName(new ComponentHolder(this.player.getProtocolVersion(), Component.text(gameProfile.getUsername())));
              playerInfoEntry.setProfile(gameProfile.getGameProfile());

              connection.delayedWrite(new UpsertPlayerInfoPacket(
                  EnumSet.of(
                      UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME,
                      UpsertPlayerInfoPacket.Action.ADD_PLAYER),
                  List.of(playerInfoEntry)));
            }

            PROFILE_FIELD.invokeExact(this.player, gameProfile.getGameProfile());

            // From Velocity.
            eventManager
                .fire(new PermissionsSetupEvent(this.player, DEFAULT_PERMISSIONS))
                .thenAcceptAsync(event -> {
                  if (!connection.isClosed()) {
                    // Wait for permissions to load, then set the players' permission function.
                    PermissionFunction function = event.createFunction(this.player);
                    if (function == null) {
                      logger.error(
                          "A plugin permission provider {} provided an invalid permission function"
                              + " for player {}. This is a bug in the plugin, not in Velocity. Falling"
                              + " back to the default permission function.",
                          event.getProvider().getClass().getName(),
                          this.player.getUsername()
                      );
                    } else {
                      try {
                        SET_PERMISSION_FUNCTION_METHOD.invokeExact(this.player, function);
                      } catch (Throwable ex) {
                        logger.error("Exception while completing injection to {}", this.player, ex);
                      }
                    }
                    try {
                      this.initialize(connection);
                    } catch (Throwable e) {
                      throw new ReflectionException(e);
                    }
                  }
                }, connection.eventLoop());
          } catch (Throwable e) {
            logger.error("Exception while completing injection to {}", this.player, e);
          }
        }, connection.eventLoop());
  }

  // From Velocity.
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void initialize(MinecraftConnection connection) throws Throwable {
    connection.setAssociation(this.player);
    if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) < 0
        || connection.getState() != StateRegistry.CONFIG) {
      this.plugin.setState(connection, StateRegistry.PLAY);
    }

    ChannelPipeline pipeline = connection.getChannel().pipeline();
    this.plugin.deject3rdParty(pipeline);

    if (pipeline.get(Connections.FRAME_ENCODER) == null) {
      this.plugin.fixCompressor(pipeline, connection.getProtocolVersion());
    }

    Logger logger = LimboAPI.getLogger();
    this.server.getEventManager().fire(new LoginEvent(this.player)).thenAcceptAsync(event -> {
      if (connection.isClosed()) {
        // The player was disconnected.
        this.server.getEventManager().fireAndForget(new DisconnectEvent(this.player, DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE));
      } else {
        Optional<Component> reason = event.getResult().getReasonComponent();
        if (reason.isPresent()) {
          this.player.disconnect0(reason.get(), false);
        } else {
          if (this.server.registerConnection(this.player)) {
            if (connection.getActiveSessionHandler() instanceof LoginConfirmHandler confirm) {
              confirm.waitForConfirmation(() -> this.connectToServer(logger, this.player, connection));
            } else {
              this.connectToServer(logger, this.player, connection);
            }
          } else {
            this.player.disconnect0(Component.translatable("velocity.error.already-connected-proxy"), false);
          }
        }
      }
    }, connection.eventLoop()).exceptionally(t -> {
      logger.error("Exception while completing login initialisation phase for {}", this.player, t);
      return null;
    });
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void connectToServer(Logger logger, ConnectedPlayer player, MinecraftConnection connection) {
    if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) < 0) {
      try {
        connection.setActiveSessionHandler(connection.getState(),
            (InitialConnectSessionHandler) INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR.invokeExact(this.player, this.server));
      } catch (Throwable e) {
        throw new ReflectionException(e);
      }
    } else if (connection.getState() == StateRegistry.PLAY) {
      // Synchronize with the client to ensure that it will not corrupt CONFIG state with PLAY packets
      ((LimboSessionHandlerImpl) connection.getActiveSessionHandler())
          .disconnectToConfig(() -> this.connectToServer(logger, player, connection));

      return; // Re-running this method due to synchronization with the client
    } else {
      ClientConfigSessionHandler configHandler = new ClientConfigSessionHandler(this.server, this.player);

      // 1.20.2+ client doesn't send ClientSettings and brand while switching state,
      // so we need to use packets that was sent during LOGIN completion.
      if (connection.getActiveSessionHandler() instanceof LimboSessionHandlerImpl sessionHandler) {
        if (sessionHandler.getSettings() != null) {
          this.player.setClientSettings(sessionHandler.getSettings());
        }

        // TODO: also queue non-vanilla plugin messages?
        if (sessionHandler.getBrand() != null) {
          try {
            this.server.getEventManager().fireAndForget(new PlayerClientBrandEvent(this.player, sessionHandler.getBrand()));
            SET_CLIENT_BRAND.invokeExact(this.player, sessionHandler.getBrand());
            BRAND_CHANNEL_SETTER.accept(configHandler, "minecraft:brand");
          } catch (Throwable e) {
            throw new ReflectionException(e);
          }
        }
      }

      this.plugin.setActiveSessionHandler(connection, StateRegistry.CONFIG, configHandler);
    }

    this.server.getEventManager().fire(new PostLoginEvent(this.player)).thenAccept(postLoginEvent -> {
      try {
        MC_CONNECTION_SETTER.accept(this.handler, connection);
        CONNECT_TO_INITIAL_SERVER_METHOD.invoke((AuthSessionHandler) this.handler, this.player);
      } catch (Throwable e) {
        throw new ReflectionException(e);
      }
    });
  }

  static {
    try {
      PROFILE_FIELD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findSetter(ConnectedPlayer.class, "profile", GameProfile.class);

      Field defaultPermissionsField = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      defaultPermissionsField.setAccessible(true);
      DEFAULT_PERMISSIONS = (PermissionProvider) defaultPermissionsField.get(null);

      SET_PERMISSION_FUNCTION_METHOD = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findVirtual(ConnectedPlayer.class, "setPermissionFunction", MethodType.methodType(void.class, PermissionFunction.class));

      INITIAL_CONNECT_SESSION_HANDLER_CONSTRUCTOR = MethodHandles
          .privateLookupIn(InitialConnectSessionHandler.class, MethodHandles.lookup())
          .findConstructor(InitialConnectSessionHandler.class, MethodType.methodType(void.class, ConnectedPlayer.class, VelocityServer.class));

      CONNECT_TO_INITIAL_SERVER_METHOD = MethodHandles.privateLookupIn(AuthSessionHandler.class, MethodHandles.lookup())
          .findVirtual(AuthSessionHandler.class, "connectToInitialServer", MethodType.methodType(CompletableFuture.class, ConnectedPlayer.class));

      Field mcConnectionField = AuthSessionHandler.class.getDeclaredField("mcConnection");
      mcConnectionField.setAccessible(true);
      MC_CONNECTION_SETTER = LambdaUtil.setterOf(mcConnectionField);

      SET_CLIENT_BRAND = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
          .findVirtual(ConnectedPlayer.class, "setClientBrand", MethodType.methodType(void.class, String.class));

      Field brandChannelField = ClientConfigSessionHandler.class.getDeclaredField("brandChannel");
      brandChannelField.setAccessible(true);
      BRAND_CHANNEL_SETTER = LambdaUtil.setterOf(brandChannelField);
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }
  }
}
