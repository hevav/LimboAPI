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

package net.elytrium.limboapi;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftCompressorAndLengthEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.elytrium.commons.config.YamlConfig;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.commons.utils.updates.UpdatesChecker;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.fastprepare.handler.PreparedPacketEncoder;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.BuiltInBiome;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualBiome;
import net.elytrium.limboapi.api.chunk.VirtualBlock;
import net.elytrium.limboapi.api.chunk.VirtualBlockEntity;
import net.elytrium.limboapi.api.chunk.VirtualChunk;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.material.Block;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PreparedPacket;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.PacketFactory;
import net.elytrium.limboapi.file.WorldFileTypeRegistry;
import net.elytrium.limboapi.injection.disconnect.DisconnectListener;
import net.elytrium.limboapi.injection.event.EventManagerHook;
import net.elytrium.limboapi.injection.login.LoginListener;
import net.elytrium.limboapi.injection.login.LoginTasksQueue;
import net.elytrium.limboapi.injection.packet.LegacyPlayerListItemHook;
import net.elytrium.limboapi.injection.packet.LimboCompressDecoder;
import net.elytrium.limboapi.injection.packet.MinecraftDiscardCompressDecoder;
import net.elytrium.limboapi.injection.packet.MinecraftLimitedCompressDecoder;
import net.elytrium.limboapi.injection.packet.PreparedPacketImpl;
import net.elytrium.limboapi.injection.packet.RemovePlayerInfoHook;
import net.elytrium.limboapi.injection.packet.UpsertPlayerInfoHook;
import net.elytrium.limboapi.material.Biome;
import net.elytrium.limboapi.protocol.LimboProtocol;
import net.elytrium.limboapi.protocol.packets.PacketFactoryImpl;
import net.elytrium.limboapi.server.CachedPackets;
import net.elytrium.limboapi.server.LimboImpl;
import net.elytrium.limboapi.server.item.SimpleItemComponentMap;
import net.elytrium.limboapi.server.world.SimpleBlock;
import net.elytrium.limboapi.server.world.SimpleBlockEntity;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.elytrium.limboapi.server.world.SimpleWorld;
import net.elytrium.limboapi.server.world.chunk.SimpleChunk;
import net.elytrium.limboapi.utils.ReloadListener;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;

@Plugin(
    id = "limboapi",
    name = "LimboAPI",
    version = "BuildConstants.LIMBO_VERSION", // TODO
    description = "Velocity plugin for making virtual servers.",
    url = "https://elytrium.net/",
    authors = {
        "Elytrium (https://elytrium.net/)",
    }
)
@SuppressFBWarnings("MS_EXPOSE_REP")
public class LimboAPI implements LimboFactory {

  private static final int SUPPORTED_MAXIMUM_PROTOCOL_VERSION_NUMBER = 768;

  // TODO translate
  /**
   * UUID на backend сервере может отличаться от того что хранится на клиенте, эта карта используется для обмана клиента подставляя ему тот uuid который клиент ожидает
   */
  private static final Map<Player, UUID> CLIENT_UUIDS = new HashMap<>();

  @MonotonicNonNull
  private static Logger LOGGER;
  @MonotonicNonNull
  private static Serializer SERIALIZER;

  private final VelocityServer server;
  private final Metrics.Factory metricsFactory;
  private final File configFile;
  private final Set<Player> players;
  private final CachedPackets packets;
  private final PacketFactory packetFactory;
  private final Map<Player, LoginTasksQueue> loginQueue;
  private final Map<Player, Function<KickedFromServerEvent, Boolean>> kickCallback;
  private final Map<Player, RegisteredServer> nextServer;

  private PreparedPacketFactory preparedPacketFactory;
  private PreparedPacketFactory configPreparedPacketFactory;
  private PreparedPacketFactory loginUncompressedPreparedPacketFactory;
  private PreparedPacketFactory loginPreparedPacketFactory;
  private ProtocolVersion minVersion;
  private ProtocolVersion maxVersion;
  private LoginListener loginListener;
  private boolean compressionEnabled;
  private EventManagerHook eventManagerHook;

  @Inject
  public LimboAPI(Logger logger, ProxyServer server, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    setLogger(logger);

    this.server = (VelocityServer) server;
    this.metricsFactory = metricsFactory;
    this.configFile = dataDirectory.resolve("config.yml").toFile();
    this.players = new HashSet<>();
    this.packetFactory = new PacketFactoryImpl();
    this.packets = new CachedPackets(this);
    this.loginQueue = new HashMap<>();
    this.kickCallback = new HashMap<>();
    this.nextServer = new HashMap<>();

    int maximumProtocolVersionNumber = ProtocolVersion.MAXIMUM_VERSION.getProtocol();
    if (maximumProtocolVersionNumber < SUPPORTED_MAXIMUM_PROTOCOL_VERSION_NUMBER) {
      LOGGER.error("Please update Velocity (https://papermc.io/downloads#Velocity). LimboAPI support: https://ely.su/discord");
      this.server.shutdown();
      return;
    } else if (maximumProtocolVersionNumber != SUPPORTED_MAXIMUM_PROTOCOL_VERSION_NUMBER) {
      LOGGER.warn(
          "Current LimboAPI version doesn't support current Velocity version (protocol version numbers: supported - {}, velocity - {})",
          SUPPORTED_MAXIMUM_PROTOCOL_VERSION_NUMBER, maximumProtocolVersionNumber
      );
      LOGGER.warn("Please update LimboAPI (https://github.com/Elytrium/LimboAPI/releases). LimboAPI support: https://ely.su/discord");
    }

    LOGGER.info("Initializing Simple Virtual World system...");
    LOGGER.info("Hooking into PlayerList/UpsertPlayerInfo and StateRegistry...");
    try {
      LegacyPlayerListItemHook.init(LimboProtocol.PLAY_CLIENTBOUND_REGISTRY);
      UpsertPlayerInfoHook.init(LimboProtocol.PLAY_CLIENTBOUND_REGISTRY);
      RemovePlayerInfoHook.init(LimboProtocol.PLAY_CLIENTBOUND_REGISTRY);
      LimboProtocol.init();
    } catch (Throwable t) {
      throw new ReflectionException(t);
    }
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    Settings.IMP.setLogger(LOGGER);

    if (Settings.IMP.reload(this.configFile, Settings.IMP.PREFIX) == YamlConfig.LoadResult.CONFIG_NOT_EXISTS) {
      LOGGER.warn("************* FIRST LAUNCH *************");
      LOGGER.warn("Thanks for installing LimboAPI!");
      LOGGER.warn("(C) 2021-2024 Elytrium");
      LOGGER.warn("");
      LOGGER.warn("Check out our plugins here: https://ely.su/github <3");
      LOGGER.warn("Discord: https://ely.su/discord");
      LOGGER.warn("****************************************");
    }

    int level = this.server.getConfiguration().getCompressionLevel();
    int threshold = this.server.getConfiguration().getCompressionThreshold();
    this.preparedPacketFactory = new PreparedPacketFactory(
        PreparedPacketImpl::new,
        LimboProtocol.getLimboStateRegistry(),
        this.compressionEnabled,
        level,
        threshold,
        Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS,
        true,
        Settings.IMP.MAIN.COMPATIBILITY_MODE
    );
    this.configPreparedPacketFactory = new PreparedPacketFactory(
        PreparedPacketImpl::new,
        StateRegistry.CONFIG,
        this.compressionEnabled,
        level,
        threshold,
        Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS,
        true,
        Settings.IMP.MAIN.COMPATIBILITY_MODE
    );
    this.loginUncompressedPreparedPacketFactory = new PreparedPacketFactory(
        PreparedPacketImpl::new,
        StateRegistry.LOGIN,
        false,
        level,
        threshold,
        false,
        true,
        Settings.IMP.MAIN.COMPATIBILITY_MODE
    );
    this.loginPreparedPacketFactory = new PreparedPacketFactory(
        PreparedPacketImpl::new,
        StateRegistry.LOGIN,
        this.compressionEnabled,
        level,
        threshold,
        Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS,
        true,
        Settings.IMP.MAIN.COMPATIBILITY_MODE
    );
    this.reloadPreparedPacketFactory();
    this.reload();

    this.metricsFactory.make(this, 12530);

    if (Settings.IMP.MAIN.CHECK_FOR_UPDATES) {
      try {
        if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/LimboAPI/master/VERSION", Settings.IMP.VERSION)) {
          LOGGER.error("****************************************");
          LOGGER.warn("The new LimboAPI update was found, please update.");
          LOGGER.error("https://github.com/Elytrium/LimboAPI/releases/");
          LOGGER.error("****************************************");
        }
      } catch (Exception e) {
        LimboAPI.LOGGER.warn("Failed to check for updates", e);
      }
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public void onPostProxyInitialize(ProxyInitializeEvent event) throws Throwable {
    this.eventManagerHook.reloadHandlers();
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "LEGACY_AMPERSAND can't be null in velocity.")
  public void reload() {
    Settings.IMP.reload(this.configFile, Settings.IMP.PREFIX);
    ComponentSerializer<Component, Component, String> serializer = Settings.IMP.SERIALIZER.getSerializer();
    if (serializer == null) {
      LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    LOGGER.info("Creating and preparing packets...");
    this.reloadVersion();
    this.packets.createPackets();
    this.loginListener = new LoginListener(this, this.server);
    this.eventManagerHook = new EventManagerHook(this, this.server.getEventManager());
    VelocityEventManager eventManager = this.server.getEventManager();
    eventManager.unregisterListeners(this);
    eventManager.register(this, this.loginListener);
    eventManager.register(this, this.eventManagerHook);
    eventManager.register(this, new DisconnectListener(this));
    eventManager.register(this, new ReloadListener(this));

    LOGGER.info("Loaded!");
  }

  private void reloadVersion() {
    this.maxVersion = Settings.IMP.MAIN.PREPARE_MAX_VERSION.equals("LATEST") ? ProtocolVersion.MAXIMUM_VERSION : ProtocolVersion.valueOf("MINECRAFT_" + Settings.IMP.MAIN.PREPARE_MAX_VERSION);
    this.minVersion = ProtocolVersion.valueOf("MINECRAFT_" + Settings.IMP.MAIN.PREPARE_MIN_VERSION);
    if (ProtocolVersion.MAXIMUM_VERSION.greaterThan(this.maxVersion) || ProtocolVersion.MINIMUM_VERSION.lessThan(this.minVersion)) {
      LOGGER.warn(
          "Currently working only with {} - {} versions, modify the plugins/limboapi/config.yml file if you want the plugin to work with other versions",
          this.minVersion.getVersionIntroducedIn(), this.maxVersion.getMostRecentSupportedVersion()
      );
    }
  }

  public void reloadPreparedPacketFactory() {
    int level = this.server.getConfiguration().getCompressionLevel();
    int threshold = this.server.getConfiguration().getCompressionThreshold();
    this.compressionEnabled = threshold != -1;
    this.preparedPacketFactory.updateCompressor(this.compressionEnabled, level, threshold, Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS, Settings.IMP.MAIN.COMPATIBILITY_MODE);
    this.configPreparedPacketFactory.updateCompressor(this.compressionEnabled, level, threshold, Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS, Settings.IMP.MAIN.COMPATIBILITY_MODE);
    this.loginPreparedPacketFactory.updateCompressor(this.compressionEnabled, level, threshold, Settings.IMP.MAIN.SAVE_UNCOMPRESSED_PACKETS, Settings.IMP.MAIN.COMPATIBILITY_MODE);
  }

  @Override
  public VirtualBlock createSimpleBlock(Block block) {
    return SimpleBlock.fromLegacyId((short) block.getId());
  }

  @Override
  public VirtualBlock createSimpleBlock(short legacyId) {
    return SimpleBlock.fromLegacyId(legacyId);
  }

  @Override
  public VirtualBlock createSimpleBlock(String modernId) {
    return SimpleBlock.fromModernId(modernId);
  }

  @Override
  public VirtualBlock createSimpleBlock(String modernId, Map<String, String> properties) {
    return SimpleBlock.fromModernId(modernId, properties);
  }

  @Override
  public VirtualBlock createSimpleBlock(short id, boolean modern) {
    return modern ? SimpleBlock.solid(id) : SimpleBlock.fromLegacyId(id);
  }

  @Override
  public VirtualBlock createSimpleBlock(short blockStateId, boolean air, boolean solid, boolean motionBlocking) {
    return new SimpleBlock(blockStateId, air, solid, motionBlocking);
  }

  @Override
  public VirtualBlock createSimpleBlock(String modernId, Map<String, String> properties, boolean air, boolean solid, boolean motionBlocking) {
    return new SimpleBlock(modernId, properties, air, solid, motionBlocking);
  }

  @Override
  public VirtualWorld createVirtualWorld(Dimension dimension, double posX, double posY, double posZ, float yaw, float pitch) {
    return new SimpleWorld(dimension, posX, posY, posZ, yaw, pitch);
  }

  @Override
  public VirtualChunk createVirtualChunk(int posX, int posZ) {
    return new SimpleChunk(posX, posZ);
  }

  @Override
  public VirtualChunk createVirtualChunk(int posX, int posZ, VirtualBiome defaultBiome) {
    return new SimpleChunk(posX, posZ, defaultBiome);
  }

  @Override
  public VirtualChunk createVirtualChunk(int posX, int posZ, BuiltInBiome defaultBiome) {
    return new SimpleChunk(posX, posZ, Biome.of(defaultBiome));
  }

  @Override
  public Limbo createLimbo(VirtualWorld world) {
    return new LimboImpl(this, world);
  }

  @Override
  public void releasePreparedPacketThread(Thread thread) {
    this.preparedPacketFactory.releaseThread(thread);
  }

  @Override
  public PreparedPacket createPreparedPacket() {
    return (PreparedPacket) this.preparedPacketFactory.createPreparedPacket(this.minVersion, this.maxVersion);
  }

  @Override
  public PreparedPacket createPreparedPacket(ProtocolVersion minVersion, ProtocolVersion maxVersion) {
    return (PreparedPacket) this.preparedPacketFactory.createPreparedPacket(minVersion, maxVersion);
  }

  @Override
  public PreparedPacket createConfigPreparedPacket() {
    return (PreparedPacket) this.configPreparedPacketFactory.createPreparedPacket(this.minVersion, this.maxVersion);
  }

  @Override
  public PreparedPacket createConfigPreparedPacket(ProtocolVersion minVersion, ProtocolVersion maxVersion) {
    return (PreparedPacket) this.configPreparedPacketFactory.createPreparedPacket(minVersion, maxVersion);
  }

  public ByteBuf encodeSingleLogin(MinecraftPacket packet, ProtocolVersion version) {
    return this.loginPreparedPacketFactory.encodeSingle(packet, version);
  }

  public ByteBuf encodeSingleLoginUncompressed(MinecraftPacket packet, ProtocolVersion version) {
    return this.loginUncompressedPreparedPacketFactory.encodeSingle(packet, version);
  }

  public void inject3rdParty(Player player, MinecraftConnection connection, ChannelPipeline pipeline) {
    StateRegistry state = connection.getState();
    if (connection.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2) || (state != StateRegistry.CONFIG && state != StateRegistry.LOGIN)) {
      this.preparedPacketFactory.inject(player, connection, pipeline);
    } else {
      this.configPreparedPacketFactory.inject(player, connection, pipeline);
    }
  }

  public void setState(MinecraftConnection connection, StateRegistry stateRegistry) {
    connection.setState(stateRegistry);
    this.setEncoderState(connection, stateRegistry);
  }

  public void setActiveSessionHandler(MinecraftConnection connection, StateRegistry stateRegistry, MinecraftSessionHandler sessionHandler) {
    connection.setActiveSessionHandler(stateRegistry, sessionHandler);
    this.setEncoderState(connection, stateRegistry);
  }

  public void setEncoderState(MinecraftConnection connection, StateRegistry state) {
    // As CONFIG state was added in 1.20.2, no need to track it for lower versions
    if (connection.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      return;
    }

    var pipeline = connection.getChannel().pipeline();
    if (Settings.IMP.MAIN.COMPATIBILITY_MODE) {
      MinecraftEncoder encoder = pipeline.get(MinecraftEncoder.class);
      if (encoder != null) {
        encoder.setState(state);
      }
    }

    PreparedPacketEncoder encoder = pipeline.get(PreparedPacketEncoder.class);
    if (encoder != null) {
      encoder.setFactory(state == StateRegistry.CONFIG || state == StateRegistry.LOGIN ? this.configPreparedPacketFactory : this.preparedPacketFactory);
    }
  }

  public void deject3rdParty(ChannelPipeline pipeline) {
    this.preparedPacketFactory.deject(pipeline);
  }

  public void fixDecompressor(ChannelPipeline pipeline, int threshold, boolean onLogin) {
    ChannelHandler decoder = onLogin && Settings.IMP.MAIN.DISCARD_COMPRESSION_ON_LOGIN || !onLogin && Settings.IMP.MAIN.DISCARD_COMPRESSION_AFTER_LOGIN
        ? new MinecraftDiscardCompressDecoder()
        : new MinecraftLimitedCompressDecoder(threshold, Natives.compress.get().create(this.server.getConfiguration().getCompressionLevel()));
    if (Settings.IMP.MAIN.COMPATIBILITY_MODE && pipeline.context(Connections.COMPRESSION_DECODER) != null) {
      pipeline.replace(Connections.COMPRESSION_DECODER, Connections.COMPRESSION_DECODER, decoder);
    } else {
      pipeline.addBefore(Connections.MINECRAFT_DECODER, Connections.COMPRESSION_DECODER, decoder);
    }
  }

  public void fixCompressor(ChannelPipeline pipeline, ProtocolVersion version) {
    ChannelHandler compressionHandler = pipeline.get(Connections.COMPRESSION_ENCODER);
    if (compressionHandler == null) {
      if (!Settings.IMP.MAIN.COMPATIBILITY_MODE) {
        pipeline.addBefore(Connections.MINECRAFT_DECODER, Connections.FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE);
      }
    } else {
      VelocityConfiguration configuration = this.server.getConfiguration();
      int compressionThreshold = configuration.getCompressionThreshold();
      VelocityCompressor compressor = Natives.compress.get().create(configuration.getCompressionLevel());
      if (!Settings.IMP.MAIN.COMPATIBILITY_MODE) {
        MinecraftCompressorAndLengthEncoder encoder = new MinecraftCompressorAndLengthEncoder(compressionThreshold, compressor);
        pipeline.remove(compressionHandler);
        pipeline.addBefore(Connections.MINECRAFT_ENCODER, Connections.COMPRESSION_ENCODER, encoder);
      }

      if (pipeline.get(Connections.COMPRESSION_DECODER) instanceof LimboCompressDecoder) {
        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(compressionThreshold, compressor);
        pipeline.replace(Connections.COMPRESSION_DECODER, Connections.COMPRESSION_DECODER, decoder);
      } else if (Settings.IMP.MAIN.COMPATIBILITY_MODE) {
        compressor.close();
      }
    }
  }

  @Override
  public void passLoginLimbo(Player player) {
    if (this.loginQueue.containsKey(player)) {
      this.loginQueue.get(player).next();
    }
  }

  @Override
  public VirtualItem getItem(Item item) {
    return SimpleItem.fromItem(item);
  }

  @Override
  public VirtualItem getItem(String modernId) {
    return SimpleItem.fromModernId(modernId);
  }

  @Override
  public VirtualItem getLegacyItem(int legacyId) {
    return SimpleItem.fromLegacyId(legacyId);
  }

  @Override
  public ItemComponentMap createItemComponentMap() {
    return new SimpleItemComponentMap();
  }

  @Override
  public VirtualBlockEntity getBlockEntityFromModernId(String modernId) {
    return SimpleBlockEntity.fromModernId(modernId);
  }

  @Override
  public VirtualBlockEntity getBlockEntityFromLegacyId(String legacyId) {
    return SimpleBlockEntity.fromLegacyId(legacyId);
  }

  @Override
  public PacketFactory getPacketFactory() {
    return this.packetFactory;
  }

  public VelocityServer getServer() {
    return this.server;
  }

  public void setLimboJoined(Player player) {
    if (!this.isLimboJoined(player)) {
      ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
      connectedPlayer.getPhase().onFirstJoin(connectedPlayer);
      this.players.add(player);
    }
  }

  public void unsetLimboJoined(Player player) {
    this.players.remove(player);
  }

  public boolean isLimboJoined(Player player) {
    return this.players.contains(player);
  }

  public CachedPackets getPackets() {
    return this.packets;
  }

  public void addLoginQueue(Player player, LoginTasksQueue queue) {
    this.loginQueue.put(player, queue);
  }

  public void removeLoginQueue(Player player) {
    this.loginQueue.remove(player);
  }

  public boolean hasLoginQueue(Player player) {
    return this.loginQueue.containsKey(player);
  }

  public LoginTasksQueue getLoginQueue(Player player) {
    return this.loginQueue.get(player);
  }

  public void setKickCallback(Player player, Function<KickedFromServerEvent, Boolean> queue) {
    this.kickCallback.put(player, queue);
  }

  public void removeKickCallback(Player player) {
    this.kickCallback.remove(player);
  }

  public Function<KickedFromServerEvent, Boolean> getKickCallback(Player player) {
    return this.kickCallback.get(player);
  }

  public void setNextServer(Player player, RegisteredServer nextServer) {
    this.nextServer.put(player, nextServer);
  }

  public void removeNextServer(Player player) {
    this.nextServer.remove(player);
  }

  public boolean hasNextServer(Player player) {
    return this.nextServer.containsKey(player);
  }

  public RegisteredServer getNextServer(Player player) {
    return this.nextServer.get(player);
  }

  public LoginListener getLoginListener() {
    return this.loginListener;
  }

  public boolean isCompressionEnabled() {
    return this.compressionEnabled;
  }

  public PreparedPacketFactory getPreparedPacketFactory() {
    return this.preparedPacketFactory;
  }

  public ProtocolVersion getPrepareMinVersion() {
    return this.minVersion;
  }

  public ProtocolVersion getPrepareMaxVersion() {
    return this.maxVersion;
  }

  public EventManagerHook getEventManagerHook() {
    return this.eventManagerHook;
  }

  @Override
  public WorldFile openWorldFile(BuiltInWorldFileType apiType, Path file) throws IOException {
    return WorldFileTypeRegistry.fromApiType(apiType, file);
  }

  @Override
  public WorldFile openWorldFile(BuiltInWorldFileType apiType, InputStream stream) throws IOException {
    return WorldFileTypeRegistry.fromApiType(apiType, stream);
  }

  @Override
  public WorldFile openWorldFile(BuiltInWorldFileType apiType, CompoundBinaryTag tag) {
    return WorldFileTypeRegistry.fromApiType(apiType, tag);
  }

  public static void setClientUniqueId(Player player, UUID clientUniqueId) {
    LimboAPI.CLIENT_UUIDS.put(player, clientUniqueId);
  }

  public static void removeClientUniqueId(Player player) {
    LimboAPI.CLIENT_UUIDS.remove(player);
  }

  public static UUID getClientUniqueId(Player player) {
    return LimboAPI.CLIENT_UUIDS.get(player);
  }

  public static UUID getClientUniqueId(UUID serverSideUniqueId) {
    for (var entry : LimboAPI.CLIENT_UUIDS.entrySet()) {
      if (entry.getKey().getUniqueId().equals(serverSideUniqueId)) {
        return entry.getValue();
      }
    }

    return serverSideUniqueId;
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  public static Logger getLogger() {
    return LOGGER;
  }

  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }
}
