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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.command.registrar.BrigadierCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.CommandRegistrar;
import com.velocitypowered.proxy.command.registrar.RawCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.SimpleCommandRegistrar;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.VelocityConnectionEvent;
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.unimi.dsi.fastutil.Pair;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limboapi.injection.login.confirmation.LoginConfirmHandler;
import net.elytrium.limboapi.injection.packet.MinecraftLimitedCompressDecoder;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packets.s2c.GameEventPacket;
import net.elytrium.limboapi.protocol.packets.s2c.DefaultSpawnPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetTimePacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetChunkCacheCenterPacket;
import net.elytrium.limboapi.protocol.util.LimboProtocolUtils;
import net.elytrium.limboapi.server.world.SimpleTagManager;
import net.elytrium.limboapi.utils.Reflection;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;

public class LimboImpl implements Limbo {

  private static final ImmutableSet<String> LEVELS = ImmutableSet.of(
      Dimension.OVERWORLD.getKey(),
      Dimension.NETHER.getKey(),
      Dimension.THE_END.getKey()
  );

  private static final MethodHandle GRACEFUL_DISCONNECT_FIELD = Reflection.findSetter(VelocityServerConnection.class, "gracefulDisconnect", boolean.class);
  private static final MethodHandle PARTIAL_HASHED_SEED_FIELD = Reflection.findSetter(JoinGamePacket.class, "partialHashedSeed", long.class);
  private static final MethodHandle LEVEL_NAMES_FIELDS = Reflection.findSetter(JoinGamePacket.class, "levelNames", ImmutableSet.class);
  private static final MethodHandle REGISTRY_FIELD = Reflection.findSetter(JoinGamePacket.class, "registry", CompoundBinaryTag.class);
  private static final MethodHandle CURRENT_DIMENSION_DATA_FIELD = Reflection.findSetter(JoinGamePacket.class, "currentDimensionData", CompoundBinaryTag.class);
  private static final MethodHandle ROOT_NODE_FIELD = Reflection.findSetter(AvailableCommandsPacket.class, "rootNode", RootCommandNode.class);

  private static final CompoundBinaryTag CHAT_TYPE_119;
  private static final CompoundBinaryTag CHAT_TYPE_1191;
  private static final CompoundBinaryTag DAMAGE_TYPE_1194;
  private static final CompoundBinaryTag DAMAGE_TYPE_120;

  private final Map<Class<? extends LimboSessionHandler>, PreparedPacket> brandMessages = new HashMap<>();
  private final LimboAPI plugin;
  private final VirtualWorld world;

  private final LongAdder currentOnline = new LongAdder();
  private final RootCommandNode<CommandSource> commandNode = new RootCommandNode<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final List<PreparedPacket> queuedToRelease = new ArrayList<>();
  private final List<CommandRegistrar<?>> registrars = ImmutableList.of(
      new BrigadierCommandRegistrar(this.commandNode, this.lock.writeLock()),
      new SimpleCommandRegistrar(this.commandNode, this.lock.writeLock()),
      new RawCommandRegistrar(this.commandNode, this.lock.writeLock())
  );

  private String limboName;
  private Integer readTimeout;
  private Long worldTicks;
  private short gameMode = GameMode.ADVENTURE.getId();
  private Integer maxSuppressPacketLength;

  private PreparedPacket joinPackets;
  private PreparedPacket fastRejoinPackets;
  private PreparedPacket safeRejoinPackets;
  private PreparedPacket postJoinPackets;
  private PreparedPacket firstChunks;
  private List<PreparedPacket> delayedChunks;
  private PreparedPacket respawnPackets;
  protected PreparedPacket configTransitionPackets;
  protected PreparedPacket configPackets;
  protected StateRegistry localStateRegistry;
  private boolean shouldRespawn = true;
  private boolean shouldRejoin = true;
  private boolean shouldUpdateTags = true;
  private boolean reducedDebugInfo = Settings.IMP.MAIN.REDUCED_DEBUG_INFO;
  private int viewDistance = Settings.IMP.MAIN.VIEW_DISTANCE;
  private int simulationDistance = Settings.IMP.MAIN.SIMULATION_DISTANCE;
  private volatile boolean built = true;
  private boolean disposeScheduled = false;

  public LimboImpl(LimboAPI plugin, VirtualWorld world) {
    this.plugin = plugin;
    this.world = world;
    this.localStateRegistry = LimboProtocol.getLimboStateRegistry();

    this.refresh();
  }

  protected void refresh() {
    JoinGamePacket legacyJoinGame = this.createLegacyJoinGamePacket();
    JoinGamePacket joinGame = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_16);
    JoinGamePacket joinGame1162 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_16_2);
    JoinGamePacket joinGame1182 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_18_2);
    JoinGamePacket joinGame119 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_19);
    JoinGamePacket joinGame1191 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_19_1);
    JoinGamePacket joinGame1194 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_19_4);
    JoinGamePacket joinGame120 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_20);
    JoinGamePacket joinGame121 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_21);
    JoinGamePacket joinGame1212 = this.createJoinGamePacket(ProtocolVersion.MINECRAFT_1_21_2);

    final PreparedPacket joinPackets = this.plugin.createPreparedPacket()
        .prepare(legacyJoinGame, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2)
        .prepare(joinGame, ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_1)
        .prepare(joinGame1162, ProtocolVersion.MINECRAFT_1_16_2, ProtocolVersion.MINECRAFT_1_18)
        .prepare(joinGame1182, ProtocolVersion.MINECRAFT_1_18_2, ProtocolVersion.MINECRAFT_1_18_2)
        .prepare(joinGame119, ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MINECRAFT_1_19)
        .prepare(joinGame1191, ProtocolVersion.MINECRAFT_1_19_1, ProtocolVersion.MINECRAFT_1_19_3)
        .prepare(joinGame1194, ProtocolVersion.MINECRAFT_1_19_4, ProtocolVersion.MINECRAFT_1_19_4)
        .prepare(joinGame120, ProtocolVersion.MINECRAFT_1_20, ProtocolVersion.MINECRAFT_1_20_5)
        .prepare(joinGame121, ProtocolVersion.MINECRAFT_1_21, ProtocolVersion.MINECRAFT_1_21)
        .prepare(joinGame1212, ProtocolVersion.MINECRAFT_1_21_2);

    PreparedPacket fastRejoinPackets = this.plugin.createPreparedPacket();
    LimboImpl.createFastClientServerSwitch(legacyJoinGame, ProtocolVersion.MINIMUM_VERSION)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINECRAFT_1_15_2));
    LimboImpl.createFastClientServerSwitch(joinGame, ProtocolVersion.MINECRAFT_1_16)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_1));
    LimboImpl.createFastClientServerSwitch(joinGame1162, ProtocolVersion.MINECRAFT_1_16_2)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_16_2, ProtocolVersion.MINECRAFT_1_18));
    LimboImpl.createFastClientServerSwitch(joinGame1182, ProtocolVersion.MINECRAFT_1_18_2)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_18_2, ProtocolVersion.MINECRAFT_1_18_2));
    LimboImpl.createFastClientServerSwitch(joinGame119, ProtocolVersion.MINECRAFT_1_19)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MINECRAFT_1_19));
    LimboImpl.createFastClientServerSwitch(joinGame1191, ProtocolVersion.MINECRAFT_1_19_1)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_19_1, ProtocolVersion.MINECRAFT_1_19_3));
    LimboImpl.createFastClientServerSwitch(joinGame1194, ProtocolVersion.MINECRAFT_1_19_4)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_19_4, ProtocolVersion.MINECRAFT_1_19_4));
    LimboImpl.createFastClientServerSwitch(joinGame120, ProtocolVersion.MINECRAFT_1_20)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_20, ProtocolVersion.MINECRAFT_1_20_5));
    LimboImpl.createFastClientServerSwitch(joinGame121, ProtocolVersion.MINECRAFT_1_21)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_21, ProtocolVersion.MINECRAFT_1_21));
    LimboImpl.createFastClientServerSwitch(joinGame1212, ProtocolVersion.MINECRAFT_1_21_2)
        .forEach(minecraftPacket -> fastRejoinPackets.prepare(minecraftPacket, ProtocolVersion.MINECRAFT_1_21_2));

    this.joinPackets = this.addPostJoin(joinPackets);
    this.fastRejoinPackets = this.addPostJoin(fastRejoinPackets);
    this.safeRejoinPackets = this.addPostJoin(this.plugin.createPreparedPacket().prepare(LimboImpl.createSafeClientServerSwitch(legacyJoinGame)));
    this.postJoinPackets = this.addPostJoin(this.plugin.createPreparedPacket());

    this.configTransitionPackets = this.plugin.createPreparedPacket()
        .prepare(StartUpdatePacket.INSTANCE, ProtocolVersion.MINECRAFT_1_20_2)
        .build();

    PreparedPacket configPackets = this.plugin.createConfigPreparedPacket();
    configPackets.prepare(this::createRegistrySyncLegacy, ProtocolVersion.MINECRAFT_1_20_2, ProtocolVersion.MINECRAFT_1_20_3);
    this.createRegistrySyncModern(configPackets, ProtocolVersion.MINECRAFT_1_20_5, ProtocolVersion.MINECRAFT_1_20_5);
    this.createRegistrySyncModern(configPackets, ProtocolVersion.MINECRAFT_1_21, ProtocolVersion.MINECRAFT_1_21);
    this.createRegistrySyncModern(configPackets, ProtocolVersion.MINECRAFT_1_21_2, ProtocolVersion.MAXIMUM_VERSION);
    if (this.shouldUpdateTags) {
      configPackets.prepare(this::createTagsUpdate, ProtocolVersion.MINECRAFT_1_20_2);
    }
    configPackets.prepare(FinishedUpdatePacket.INSTANCE, ProtocolVersion.MINECRAFT_1_20_2);
    this.configPackets = configPackets.build();

    this.firstChunks = this.createFirstChunks();
    this.delayedChunks = this.createDelayedChunksPackets();
    PreparedPacket respawnPackets = this.plugin.createPreparedPacket()
        .prepare(LimboImpl.createPlayerPosition(this.world.getSpawnX(), this.world.getSpawnY(), this.world.getSpawnZ(), this.world.getYaw(), this.world.getPitch()))
        .prepare(LimboImpl.createSetChunkCacheCenter((int) this.world.getSpawnX(), (int) this.world.getSpawnZ()), ProtocolVersion.MINECRAFT_1_14);

    if (this.shouldUpdateTags) {
      respawnPackets.prepare(SimpleTagManager::getUpdateTagsPacket, ProtocolVersion.MINECRAFT_1_13, ProtocolVersion.MINECRAFT_1_20);
    }

    this.respawnPackets = respawnPackets.build();
    this.built = true;
  }

  private GameEventPacket createLevelChunksLoadStartGameEvent() {
    return new GameEventPacket(13, 0); // LEVEL_CHUNKS_LOAD_START
  }

  private RegistrySyncPacket createRegistrySyncLegacy(ProtocolVersion version) {
    ByteBuf encodedRegistry = this.plugin.getPreparedPacketFactory().getPreparedPacketAllocator().ioBuffer();
    ProtocolUtils.writeBinaryTag(encodedRegistry, version, LimboImpl.createRegistry(version));

    RegistrySyncPacket sync = new RegistrySyncPacket();
    sync.replace(encodedRegistry);
    return sync;
  }

  @SuppressWarnings("unchecked")
  private void createRegistrySyncModern(PreparedPacket packet, ProtocolVersion from, ProtocolVersion to) {
    CompoundBinaryTag registryTag = LimboImpl.createRegistry(from);
    for (String key : registryTag.keySet()) {
      CompoundBinaryTag entry = registryTag.getCompound(key);

      String type = entry.getString("type");
      ListBinaryTag values = entry.getList("value", BinaryTagTypes.COMPOUND);

      Pair<String, BinaryTag> emptyTag = null;
      Pair<String, BinaryTag>[] tags = new Pair[0];

      for (BinaryTag elementTag : values) {
        CompoundBinaryTag element = (CompoundBinaryTag) elementTag;
        int id = element.getInt("id");
        if (id >= tags.length) {
          tags = Arrays.copyOf(tags, id + 1);
        }

        tags[id] = Pair.of(element.getString("name"), element.getCompound("element"));
        if (emptyTag == null) {
          emptyTag = tags[id];
        }
      }

      for (int i = 0; i < tags.length; ++i) {
        if (tags[i] == null) {
          tags[i] = Pair.of("limboapi_padding_" + i, emptyTag.value());
        }
      }

      var patchedTags = tags;
      packet.prepare(version -> {
        ByteBuf registry = this.plugin.getPreparedPacketFactory().getPreparedPacketAllocator().ioBuffer();

        ProtocolUtils.writeString(registry, type);
        LimboProtocolUtils.writeArray(registry, patchedTags, pair -> {
          ProtocolUtils.writeString(registry, pair.left());
          BinaryTag tag = pair.right();
          if (tag == null) {
            registry.writeBoolean(false);
          } else {
            registry.writeBoolean(true);
            ProtocolUtils.writeBinaryTag(registry, version, tag);
          }
        });

        RegistrySyncPacket sync = new RegistrySyncPacket();
        sync.replace(registry);
        return sync;
      }, from, to);
    }
  }

  private TagsUpdatePacket createTagsUpdate(ProtocolVersion version) {
    return new TagsUpdatePacket(SimpleTagManager.getUpdateTagsPacket(version).tags());
  }

  private PreparedPacket addPostJoin(PreparedPacket packet) {
    return packet.prepare(this.createAvailableCommandsPacket(), ProtocolVersion.MINECRAFT_1_13)
        .prepare(this.createDefaultSpawnPositionPacket())
        .prepare(this.createLevelChunksLoadStartGameEvent(), ProtocolVersion.MINECRAFT_1_20_3)
        .prepare(this.createWorldTicksPacket())
        .prepare(this::createBrandMessage)
        .build();
  }

  @Override
  public void spawnPlayer(Player apiPlayer, LimboSessionHandler handler) { // TODO prevent double spawnPlayer without proper disconnect (?) (я уже не помню откуда это и при каких условиях возникает)
    if (!this.built) {
      synchronized (this) {
        if (!this.built) {
          List<PreparedPacket> packets = this.takeSnapshot();
          try {
            this.refresh();
          } finally {
            List<PreparedPacket> changed = this.takeSnapshot();
            for (PreparedPacket packet : packets) {
              if (!changed.contains(packet)) {
                this.queuedToRelease.add(packet);
              }
            }
          }
        }
      }
    }

    ConnectedPlayer player = (ConnectedPlayer) apiPlayer;
    MinecraftConnection connection = player.getConnection();

    boolean shouldSpawnPlayerImmediately = true;

    // Discard information from previous server
    if (connection.getActiveSessionHandler() instanceof ClientPlaySessionHandler sessionHandler) {
      connection.eventLoop().execute(() -> {
        player.getTabList().clearAll();
        for (UUID serverBossBar : sessionHandler.getServerBossBars()) {
          connection.delayedWrite(BossBarPacket.createRemovePacket(serverBossBar, null));
        }
        sessionHandler.getServerBossBars().clear();

        if (player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
          connection.delayedWrite(GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.RESET, player.getProtocolVersion()));
          player.clearPlayerListHeaderAndFooter();
        }

        connection.flush();
      });
    }

    if (connection.getState() != this.localStateRegistry) {
      if (connection.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        connection.eventLoop().execute(() -> connection.setState(this.localStateRegistry));
      }
      VelocityServerConnection server = player.getConnectedServer();
      if (server != null) {
        RegisteredServer previousServer = server.getServer();
        MinecraftConnection serverConnection = server.getConnection();
        if (serverConnection != null) {
          try {
            GRACEFUL_DISCONNECT_FIELD.invokeExact(server, true);
          } catch (Throwable t) {
            throw new ReflectionException(t);
          }

          connection.eventLoop().execute(() -> serverConnection.getChannel().close().addListener(f -> this.spawnPlayerLocal(player, handler, previousServer)));
          shouldSpawnPlayerImmediately = false;
        }

        player.setConnectedServer(null);
        this.plugin.setLimboJoined(player);
      }
    }

    if (shouldSpawnPlayerImmediately) {
      this.spawnPlayerLocal(player, handler, null);
    }
  }

  protected void spawnPlayerLocal(Class<? extends LimboSessionHandler> handlerClass, LimboSessionHandlerImpl sessionHandler, ConnectedPlayer player, MinecraftConnection connection) {
    if (!connection.eventLoop().inEventLoop()) {
      connection.eventLoop().execute(() -> this.spawnPlayerLocal(handlerClass, sessionHandler, player, connection));
      return;
    }

    connection.setActiveSessionHandler(connection.getState(), sessionHandler);
    ProtocolVersion version = connection.getProtocolVersion();
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      if (connection.getState() != StateRegistry.CONFIG) {
        if (this.shouldRejoin) {
          // Switch to CONFIG state
          connection.write(this.configTransitionPackets);
          return; // Continue transition on the handler side
        }
      } else {
        // Switch to PLAY state
        connection.delayedWrite(this.configPackets);
        this.plugin.setEncoderState(connection, this.localStateRegistry);
      }
    }

    sessionHandler.onConfig(new LimboPlayerImpl(this.plugin, this, player));

    if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_2) || (connection.getState() != StateRegistry.CONFIG && !this.shouldRejoin)) {
      this.onSpawn(handlerClass, connection, player, sessionHandler);
    }

    connection.flush();
  }

  @SuppressWarnings("UnnecessaryToStringCall")
  private void spawnPlayerLocal(ConnectedPlayer player, LimboSessionHandler handler, RegisteredServer previousServer) {
    MinecraftConnection connection = player.getConnection();
    connection.eventLoop().execute(() -> {
      connection.flush();

      ChannelPipeline pipeline = connection.getChannel().pipeline();
      Class<? extends LimboSessionHandler> handlerClass = handler.getClass();
      if (this.limboName == null) {
        this.limboName = handlerClass.getSimpleName();
      }

      if (Settings.IMP.MAIN.LOGGING_ENABLED) {
        LimboAPI.getLogger().info("{} has connected to the {} Limbo", player.toString(), this.limboName);
      }

      // With an abnormally large number of connections from the same nickname,
      // requests don't have time to be processed, and an error occurs that "minecraft-encoder" doesn't exist
      if (pipeline.get(Connections.MINECRAFT_ENCODER) != null) {
        if (this.readTimeout != null) {
          pipeline.replace(Connections.READ_TIMEOUT, LimboProtocol.READ_TIMEOUT, new ReadTimeoutHandler(this.readTimeout, TimeUnit.MILLISECONDS));
        }

        boolean compressionEnabled = false;
        if (pipeline.get(PreparedPacketFactory.PREPARED_ENCODER) == null) {
          if (this.plugin.isCompressionEnabled() && connection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
            if (pipeline.get(Connections.FRAME_ENCODER) != null) {
              if (!Settings.IMP.MAIN.COMPATIBILITY_MODE) {
                pipeline.remove(Connections.FRAME_ENCODER);
              }
            } else if (Settings.IMP.MAIN.COMPATIBILITY_MODE) {
              if (pipeline.context(Connections.COMPRESSION_DECODER) != null) {
                this.plugin.fixDecompressor(pipeline, this.plugin.getServer().getConfiguration().getCompressionThreshold(), false);
              }
            } else if (pipeline.remove(Connections.COMPRESSION_DECODER) != null) {
              this.plugin.fixDecompressor(pipeline, this.plugin.getServer().getConfiguration().getCompressionThreshold(), false);
              pipeline.replace(Connections.COMPRESSION_ENCODER, Connections.COMPRESSION_ENCODER, new ChannelOutboundHandlerAdapter());
              compressionEnabled = true;
            }
          } else if (!Settings.IMP.MAIN.COMPATIBILITY_MODE) {
            pipeline.remove(Connections.FRAME_ENCODER);
          }

          this.plugin.inject3rdParty(player, connection, pipeline);
          pipeline.fireUserEventTriggered(compressionEnabled ? VelocityConnectionEvent.COMPRESSION_ENABLED : VelocityConnectionEvent.COMPRESSION_DISABLED);
        }
      } else {
        connection.close();
        return;
      }

      if (this.maxSuppressPacketLength != null) {
        MinecraftLimitedCompressDecoder decoder = pipeline.get(MinecraftLimitedCompressDecoder.class);
        if (decoder != null) {
          decoder.setUncompressedCap(this.maxSuppressPacketLength);
        }
      }

      LimboSessionHandlerImpl sessionHandler = new LimboSessionHandlerImpl(this.plugin, this, player, handler, connection.getState(), connection.getActiveSessionHandler(), previousServer);
      if (connection.getActiveSessionHandler() instanceof LoginConfirmHandler confirm) {
        confirm.waitForConfirmation(() -> {
          this.currentOnline.increment();
          this.spawnPlayerLocal(handlerClass, sessionHandler, player, connection);
        });
      } else {
        this.currentOnline.increment();
        this.spawnPlayerLocal(handlerClass, sessionHandler, player, connection);
      }
    });
  }

  protected void onSpawn(Class<? extends LimboSessionHandler> handlerClass, MinecraftConnection connection, ConnectedPlayer player, LimboSessionHandlerImpl sessionHandler) {
    if (this.plugin.isLimboJoined(player)) {
      if (this.shouldRejoin) {
        sessionHandler.setJoinGameTriggered(true);
        if (connection.getType() == ConnectionTypes.LEGACY_FORGE) {
          connection.delayedWrite(this.safeRejoinPackets);
        } else {
          connection.delayedWrite(this.fastRejoinPackets);
        }
      } else {
        connection.delayedWrite(this.postJoinPackets);
      }
    } else {
      sessionHandler.setJoinGameTriggered(true);
      connection.delayedWrite(this.joinPackets);
    }

    UUID uuid = LimboAPI.getClientUniqueId(player);
    MinecraftPacket playerInfoPacket;
    if (connection.getProtocolVersion().noGreaterThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      playerInfoPacket = new LegacyPlayerListItemPacket(LegacyPlayerListItemPacket.ADD_PLAYER,
          List.of(new LegacyPlayerListItemPacket.Item(uuid).setName(player.getUsername()).setGameMode(this.gameMode).setProperties(player.getGameProfileProperties()))
      );
    } else {
      UpsertPlayerInfoPacket.Entry playerInfoEntry = new UpsertPlayerInfoPacket.Entry(uuid);
      playerInfoEntry.setDisplayName(new ComponentHolder(player.getProtocolVersion(), Component.text(player.getUsername())));
      playerInfoEntry.setGameMode(this.gameMode);
      playerInfoEntry.setProfile(player.getGameProfile());
      playerInfoPacket = new UpsertPlayerInfoPacket(
          EnumSet.of(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE, UpsertPlayerInfoPacket.Action.ADD_PLAYER),
          List.of(playerInfoEntry)
      );
    }

    connection.delayedWrite(playerInfoPacket);

    connection.delayedWrite(this.getBrandMessage(handlerClass));

    this.plugin.setLimboJoined(player);

    if (this.shouldRespawn) {
      this.respawnPlayer(player);
    }

    sessionHandler.onSpawn();
  }

  @Override
  public void respawnPlayer(Player player) {
    MinecraftConnection connection = ((ConnectedPlayer) player).getConnection();

    connection.delayedWrite(this.respawnPackets);
    if (this.firstChunks != null) {
      connection.write(this.firstChunks);
    }

    List<PreparedPacket> chunksSnapshot = this.delayedChunks;
    if (!chunksSnapshot.isEmpty()) {
      ScheduledFuture<?>[] task = new ScheduledFuture<?>[1];
      task[0] = connection.eventLoop().scheduleAtFixedRate(new Runnable() {

        private int index;

        @Override
        public void run() {
          if (connection.isClosed()) {
            task[0].cancel(false);
            return;
          }

          connection.write(chunksSnapshot.get(this.index));
          if (++this.index >= chunksSnapshot.size()) {
            task[0].cancel(false);
          }
        }
      }, 50, 50, TimeUnit.MILLISECONDS);
      if (connection.getActiveSessionHandler() instanceof LimboSessionHandlerImpl sessionHandler) {
        sessionHandler.setRespawnTask(task[0]);
      }
    }
  }

  @Override
  public long getCurrentOnline() {
    return this.currentOnline.sum();
  }

  public void onDisconnect() {
    this.currentOnline.decrement();

    if (!this.queuedToRelease.isEmpty() && this.currentOnline.sum() == 0) {
      synchronized (this) {
        PreparedPacket[] packets = this.queuedToRelease.toArray(new PreparedPacket[0]);
        this.queuedToRelease.clear();

        // Wait some time to ensure that queued packets is really unused
        this.plugin.getServer().getScheduler().buildTask(this.plugin, () -> {
          for (PreparedPacket packet : packets) {
            packet.release();
          }
        }).delay(10, TimeUnit.SECONDS).schedule();
      }
    }

    if (this.disposeScheduled && this.currentOnline.sum() == 0) {
      this.localDispose();
    }
  }

  @Override
  public Limbo setName(String name) {
    this.limboName = name;
    this.built = false;
    return this;
  }

  public String getName() {
    return this.limboName;
  }

  @Override
  public Limbo setReadTimeout(int millis) {
    this.readTimeout = millis;
    return this;
  }

  @Override
  public Limbo setWorldTime(long ticks) {
    this.worldTicks = ticks;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setGameMode(GameMode gameMode) {
    this.gameMode = gameMode.getId();
    this.built = false;
    return this;
  }

  @Override
  public Limbo setShouldRejoin(boolean shouldRejoin) {
    this.shouldRejoin = shouldRejoin;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setShouldRespawn(boolean shouldRespawn) {
    this.shouldRespawn = shouldRespawn;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setShouldUpdateTags(boolean shouldUpdateTags) {
    this.shouldUpdateTags = shouldUpdateTags;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setReducedDebugInfo(boolean reducedDebugInfo) {
    this.reducedDebugInfo = reducedDebugInfo;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setViewDistance(int viewDistance) {
    this.viewDistance = viewDistance;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setSimulationDistance(int simulationDistance) {
    this.simulationDistance = simulationDistance;
    this.built = false;
    return this;
  }

  @Override
  public Limbo setMaxSuppressPacketLength(int maxSuppressPacketLength) {
    this.maxSuppressPacketLength = maxSuppressPacketLength;
    return this;
  }

  @Override
  public Limbo registerCommand(CommandMeta commandMeta) {
    return this.registerCommand(commandMeta, (SimpleCommand) invocation -> {
      // Do nothing
    });
  }

  @Override
  public Limbo registerCommand(CommandMeta commandMeta, Command command) {
    for (CommandRegistrar<?> registrar : this.registrars) {
      if (LimboImpl.tryRegister(registrar, commandMeta, command)) {
        this.built = false;
        return this;
      }
    }

    throw new IllegalArgumentException(command + " does not implement a registrable Command sub-interface");
  }

  public Limbo registerPacket(PacketDirection direction, Class<?> packetClass, Supplier<?> packetSupplier, PacketMapping[] packetMappings) {
    if (this.localStateRegistry == LimboProtocol.getLimboStateRegistry()) {
      this.localStateRegistry = LimboProtocol.createLocalStateRegistry();
      this.plugin.getPreparedPacketFactory().addStateRegistry(this.localStateRegistry);
    }

    LimboProtocol.register(this.localStateRegistry, direction, packetClass, packetSupplier, packetMappings);
    return this;
  }

  @Override
  public void dispose() {
    if (this.getCurrentOnline() == 0) {
      this.localDispose();
    } else {
      this.disposeScheduled = true;
    }
  }

  private List<PreparedPacket> takeSnapshot() {
    List<PreparedPacket> packets = new ArrayList<>();
    if (this.joinPackets != null) {
      packets.add(this.joinPackets);
    }
    if (this.fastRejoinPackets != null) {
      packets.add(this.fastRejoinPackets);
    }
    if (this.safeRejoinPackets != null) {
      packets.add(this.safeRejoinPackets);
    }
    if (this.respawnPackets != null) {
      packets.add(this.respawnPackets);
    }
    if (this.firstChunks != null) {
      packets.add(this.firstChunks);
    }
    if (this.delayedChunks != null) {
      packets.addAll(this.delayedChunks);
    }
    if (this.configTransitionPackets != null) {
      packets.add(this.configTransitionPackets);
    }
    if (this.configPackets != null) {
      packets.add(this.configPackets);
    }
    return packets;
  }

  private void localDispose() {
    this.takeSnapshot().forEach(PreparedPacket::release);
  }

  // From Velocity
  private static <T extends Command> boolean tryRegister(CommandRegistrar<T> registrar, CommandMeta commandMeta, Command command) {
    Class<T> superInterface = registrar.registrableSuperInterface();
    if (superInterface.isInstance(command)) {
      registrar.register(commandMeta, superInterface.cast(command));
      return true;
    }

    return false;
  }

  private static CompoundBinaryTag createRegistry(String registryName, Map<String, CompoundBinaryTag> tags) {
    int id = 0;

    ListBinaryTag.Builder<CompoundBinaryTag> builder = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
    for (Entry<String, CompoundBinaryTag> tag : tags.entrySet()) {
      builder.add(CompoundBinaryTag.builder()
          .putString("name", tag.getKey())
          .putInt("id", id++)
          .put("element", tag.getValue())
          .build()
      );
    }

    return LimboImpl.createRegistry(registryName, builder.build());
  }

  private static CompoundBinaryTag createRegistry(String registryName, ListBinaryTag tags) {
    return CompoundBinaryTag.builder()
        .putString("type", registryName)
        .put("value", tags)
        .build();
  }

  private static CompoundBinaryTag createDimensionData(Dimension dimension, ProtocolVersion version) {
    CompoundBinaryTag details = CompoundBinaryTag.builder()
        .putBoolean("natural", false)
        .putFloat("ambient_light", 0.0F)
        .putBoolean("shrunk", false)
        .putBoolean("ultrawarm", false)
        .putBoolean("has_ceiling", false)
        .putBoolean("has_skylight", true)
        .putBoolean("piglin_safe", false)
        .putBoolean("bed_works", false)
        .putBoolean("respawn_anchor_works", false)
        .putBoolean("has_raids", false)
        .putInt("logical_height", 256)
        .putString("infiniburn", version.noLessThan(ProtocolVersion.MINECRAFT_1_18_2) ? "#minecraft:infiniburn_nether" : "minecraft:infiniburn_nether")
        .putDouble("coordinate_scale", 1.0)
        .putString("effects", dimension.getKey())
        .putInt("min_y", 0)
        .putInt("height", 256)
        .putInt("monster_spawn_block_light_limit", 0)
        .putInt("monster_spawn_light_level", 0)
        .build();
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)
        ? CompoundBinaryTag.builder()
            .putString("name", dimension.getKey())
            .putInt("id", dimension.getModernId())
            .put("element", details)
            .build()
        : details.putString("name", dimension.getKey());
  }

  private JoinGamePacket createJoinGamePacket(ProtocolVersion version) {
    Dimension dimension = this.world.getDimension();
    JoinGamePacket joinGame = new JoinGamePacket();
    joinGame.setEntityId(1);
    joinGame.setIsHardcore(true);
    joinGame.setGamemode(this.gameMode);
    joinGame.setPreviousGamemode((short) -1);
    joinGame.setDimension(dimension.getModernId());
    joinGame.setDifficulty((short) 0);
    try {
      PARTIAL_HASHED_SEED_FIELD.invokeExact(joinGame, ThreadLocalRandom.current().nextLong());
    } catch (Throwable t) {
      throw new ReflectionException(t);
    }
    joinGame.setMaxPlayers(1);

    joinGame.setLevelType("flat");

    joinGame.setViewDistance(this.viewDistance);
    joinGame.setSimulationDistance(this.simulationDistance);

    joinGame.setReducedDebugInfo(this.reducedDebugInfo);

    String key = dimension.getKey();
    joinGame.setDimensionInfo(new DimensionInfo(key, key, false, false, version));

    try {
      ListBinaryTag dimensionRegistry = LimboImpl.createDimensionRegistry(version);
      CompoundBinaryTag currentDimensionData = dimensionRegistry.getCompound(dimension.getModernId());
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        currentDimensionData = currentDimensionData.getCompound("element");
      }

      LEVEL_NAMES_FIELDS.invokeExact(joinGame, LEVELS);
      REGISTRY_FIELD.invokeExact(joinGame, LimboImpl.createRegistry(version, dimensionRegistry));
      CURRENT_DIMENSION_DATA_FIELD.invokeExact(joinGame, currentDimensionData);
    } catch (Throwable t) {
      throw new ReflectionException(t);
    }

    return joinGame;
  }

  private static CompoundBinaryTag createRegistry(ProtocolVersion version) {
    return LimboImpl.createRegistry(version, LimboImpl.createDimensionRegistry(version));
  }

  private static CompoundBinaryTag createRegistry(ProtocolVersion version, ListBinaryTag dimensionRegistry) {
    CompoundBinaryTag.Builder registry = CompoundBinaryTag.builder();
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
      registry.put("minecraft:dimension_type", CompoundBinaryTag.builder().putString("type", "minecraft:dimension_type").put("value", dimensionRegistry).build());
      registry.put("minecraft:worldgen/biome", Biome.getRegistry(version));
      if (version == ProtocolVersion.MINECRAFT_1_19) {
        registry.put("minecraft:chat_type", CHAT_TYPE_119);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        registry.put("minecraft:chat_type", CHAT_TYPE_1191);
      }

      // TODO: Generate mappings for damage_type registry
      if (version == ProtocolVersion.MINECRAFT_1_19_4) {
        registry.put("minecraft:damage_type", DAMAGE_TYPE_1194);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21)) {
        ListBinaryTag values = DAMAGE_TYPE_120.getList("value");

        ListBinaryTag.Builder<CompoundBinaryTag> tags = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
        for (BinaryTag tag : values) {
          tags.add((CompoundBinaryTag) tag);
        }

        int id = values.size();
        for (String name : version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)
            ? new String[] {"minecraft:campfire", "minecraft:ender_pearl", "minecraft:mace_smash"}
            : new String[] {"minecraft:campfire"}) {
          tags.add(CompoundBinaryTag.builder()
              .putString("name", name)
              .putInt("id", id++)
              .put("element", values.getCompound(0).getCompound("element"))
              .build()
          );
        }

        registry.put("minecraft:damage_type", LimboImpl.createRegistry("minecraft:damage_type", tags.build()));
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
        registry.put("minecraft:damage_type", DAMAGE_TYPE_120);
      }

      // TODO: Generate mappings for painting_variant and wolf_variant registries
      // TODO: API
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        // TODO мб ещё сюда и варианты для баннеров добавлять (если их в регистре не отправит, то они пустыми будут)
        registry.put("minecraft:wolf_variant", LimboImpl.createRegistry("minecraft:wolf_variant", Map.of(
            "minecraft:ashen", CompoundBinaryTag.builder()
                .putString("wild_texture", "minecraft:entity/wolf/wolf_ashen")
                .putString("tame_texture", "minecraft:entity/wolf/wolf_ashen_tame")
                .putString("angry_texture", "minecraft:entity/wolf/wolf_ashen_angry")
                .put("biomes", ListBinaryTag.builder().add(StringBinaryTag.stringBinaryTag("minecraft:plains")).build())
                .build()
        )));
      }
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21)) {
        registry.put("minecraft:painting_variant", LimboImpl.createRegistry("minecraft:painting_variant", Map.of(
            "minecraft:alban", CompoundBinaryTag.builder()
                .putInt("width", 1)
                .putInt("height", 1)
                .putString("asset_id", "minecraft:alban")
                .build()
        )));
      }
    } else {
      registry.put("dimension", dimensionRegistry);
    }

    return registry.build();
  }

  private static ListBinaryTag createDimensionRegistry(ProtocolVersion version) {
    return ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
        .add(LimboImpl.createDimensionData(Dimension.OVERWORLD, version))
        .add(LimboImpl.createDimensionData(Dimension.NETHER, version))
        .add(LimboImpl.createDimensionData(Dimension.THE_END, version))
        .build();
  }

  private JoinGamePacket createLegacyJoinGamePacket() {
    JoinGamePacket joinGame = this.createJoinGamePacket(ProtocolVersion.MINIMUM_VERSION);
    joinGame.setDimension(this.world.getDimension().getLegacyId());
    return joinGame;
  }

  private DefaultSpawnPositionPacket createDefaultSpawnPositionPacket() {
    return new DefaultSpawnPositionPacket((int) this.world.getSpawnX(), (int) this.world.getSpawnY(), (int) this.world.getSpawnZ(), 0.0F);
  }

  private SetTimePacket createWorldTicksPacket() {
    return this.worldTicks == null ? null : new SetTimePacket(this.worldTicks, this.worldTicks);
  }

  private AvailableCommandsPacket createAvailableCommandsPacket() {
    try {
      AvailableCommandsPacket packet = new AvailableCommandsPacket();
      ROOT_NODE_FIELD.invokeExact(packet, this.commandNode);
      return packet;
    } catch (Throwable t) {
      throw new ReflectionException(t);
    }
  }

  private PreparedPacket createFirstChunks() {
    PreparedPacket packet = this.plugin.createPreparedPacket();
    var orderedChunks = this.world.getOrderedChunks();

    int chunkCounter = 0;
    for (List<VirtualChunk> chunksWithSameDistance : orderedChunks) {
      if (++chunkCounter > Settings.IMP.MAIN.CHUNK_RADIUS_SEND_ON_SPAWN) {
        break;
      }

      for (VirtualChunk chunk : chunksWithSameDistance) {
        this.plugin.getPacketFactory().prepareCompleteChunkDataPacket(this.plugin.getPrepareMinVersion(), this.plugin.getPrepareMaxVersion(), packet, chunk.createSnapshot(true), this.world.getDimension());
      }
    }

    return packet.build();
  }

  private List<PreparedPacket> createDelayedChunksPackets() {
    var orderedChunks = this.world.getOrderedChunks();
    if (orderedChunks.size() <= Settings.IMP.MAIN.CHUNK_RADIUS_SEND_ON_SPAWN) {
      return List.of();
    }

    List<PreparedPacket> packets = new LinkedList<>();
    PreparedPacket packet = this.plugin.createPreparedPacket();
    int chunkCounter = 0;

    var distanceIterator = orderedChunks.listIterator();
    for (int i = 0; i < Settings.IMP.MAIN.CHUNK_RADIUS_SEND_ON_SPAWN; ++i) {
      distanceIterator.next();
    }

    while (distanceIterator.hasNext()) {
      for (VirtualChunk chunk : distanceIterator.next()) {
        if (++chunkCounter > Settings.IMP.MAIN.CHUNKS_PER_TICK) {
          packets.add(packet.build());
          packet = this.plugin.createPreparedPacket();
          chunkCounter = 0;
        }

        this.plugin.getPacketFactory().prepareCompleteChunkDataPacket(this.plugin.getPrepareMinVersion(), this.plugin.getPrepareMaxVersion(), packet, chunk.createSnapshot(true), this.world.getDimension());
      }
    }

    packets.add(packet.build());
    return packets;
  }

  // From Velocity
  private static List<MinecraftPacket> createFastClientServerSwitch(JoinGamePacket joinGame, ProtocolVersion version) {
    // In order to handle switching to another server, you will need to send two packets:
    //
    // - The join game packet from the backend server, with a different dimension.
    // - A respawn with the correct dimension.
    //
    // Most notably, by having the client accept the join game packet, we can work around the need
    // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
    // improving compatibility with mods.
    RespawnPacket respawn = RespawnPacket.fromJoinGame(joinGame);
    if (version.lessThan(ProtocolVersion.MINECRAFT_1_16)) {
      // Before Minecraft 1.16, we could not switch to the same dimension without sending an
      // additional respawn. On older versions of Minecraft this forces the client to perform
      // garbage collection which adds additional latency.
      joinGame.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    }

    return List.of(joinGame, respawn);
  }

  private static List<MinecraftPacket> createSafeClientServerSwitch(JoinGamePacket joinGame) {
    // Some clients do not behave well with the "fast" respawn sequence. In this case we will use
    // a "safe" respawn sequence that involves sending three packets to the client. They have the
    // same effect but tend to work better with buggier clients (Forge 1.8 in particular).
    List<MinecraftPacket> packets = new ArrayList<>();

    // Send the JoinGame packet itself, unmodified.
    packets.add(joinGame);

    // Send a respawn packet in a different dimension.
    RespawnPacket fakeSwitchPacket = RespawnPacket.fromJoinGame(joinGame);
    fakeSwitchPacket.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    packets.add(fakeSwitchPacket);

    // Now send a respawn packet in the correct dimension.
    RespawnPacket correctSwitchPacket = RespawnPacket.fromJoinGame(joinGame);
    packets.add(correctSwitchPacket);

    return packets;
  }

  private PreparedPacket getBrandMessage(Class<? extends LimboSessionHandler> clazz) {
    if (this.brandMessages.containsKey(clazz)) {
      return this.brandMessages.get(clazz);
    } else {
      PreparedPacket preparedPacket = this.plugin.createPreparedPacket().prepare(this::createBrandMessage).build();
      this.brandMessages.put(clazz, preparedPacket);
      return preparedPacket;
    }
  }

  private PluginMessagePacket createBrandMessage(ProtocolVersion version) {
    String brand = "LimboAPI (" + Settings.IMP.VERSION + ") -> " + this.limboName;
    ByteBuf buf = Unpooled.buffer(2 + brand.length());
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeCharSequence(brand, StandardCharsets.UTF_8);
    } else {
      ProtocolUtils.writeString(buf, brand);
    }

    return new PluginMessagePacket("MC|Brand", buf);
  }

  private static PlayerPositionPacket createPlayerPosition(double posX, double posY, double posZ, float yaw, float pitch) {
    return new PlayerPositionPacket(posX, posY, posZ, yaw, pitch, false, 44, true);
  }

  private static SetChunkCacheCenterPacket createSetChunkCacheCenter(int posX, int posZ) {
    return new SetChunkCacheCenterPacket(posX >> 4, posZ >> 4);
  }

  public Integer getReadTimeout() {
    return this.readTimeout;
  }

  static {
    try {
      BinaryTagIO.Reader reader = BinaryTagIO.unlimitedReader();
      try (InputStream stream = LimboAPI.class.getResourceAsStream("/mappings/chat_type_1_19.nbt")) {
        CHAT_TYPE_119 = reader.read(Objects.requireNonNull(stream), BinaryTagIO.Compression.GZIP);
      }
      try (InputStream stream = LimboAPI.class.getResourceAsStream("/mappings/chat_type_1_19_1.nbt")) {
        CHAT_TYPE_1191 = reader.read(Objects.requireNonNull(stream), BinaryTagIO.Compression.GZIP);
      }
      try (InputStream stream = LimboAPI.class.getResourceAsStream("/mappings/damage_type_1_19_4.nbt")) {
        DAMAGE_TYPE_1194 = reader.read(Objects.requireNonNull(stream), BinaryTagIO.Compression.GZIP);
      }
      try (InputStream stream = LimboAPI.class.getResourceAsStream("/mappings/damage_type_1_20.nbt")) {
        DAMAGE_TYPE_120 = reader.read(Objects.requireNonNull(stream), BinaryTagIO.Compression.GZIP);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
