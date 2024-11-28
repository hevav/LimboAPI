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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConfigSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.material.Item;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limboapi.api.protocol.packets.data.AbilityFlags;
import net.elytrium.limboapi.api.protocol.packets.data.EntityDataValue;
import net.elytrium.limboapi.api.protocol.packets.data.MapData;
import net.elytrium.limboapi.api.protocol.packets.data.MapPalette;
import net.elytrium.limboapi.protocol.packets.s2c.GameEventPacket;
import net.elytrium.limboapi.protocol.packets.s2c.MapDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerAbilitiesPacket;
import net.elytrium.limboapi.protocol.packets.s2c.PlayerPositionPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetEntityDataPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetSlotPacket;
import net.elytrium.limboapi.protocol.packets.s2c.SetTimePacket;
import net.elytrium.limboapi.server.world.SimpleItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LimboPlayerImpl implements LimboPlayer {

  private final LimboAPI plugin;
  private final LimboImpl server;
  private final ConnectedPlayer player;
  private final MinecraftConnection connection;
  private final LimboSessionHandlerImpl sessionHandler;

  private GameMode gameMode = GameMode.ADVENTURE;

  public LimboPlayerImpl(LimboAPI plugin, LimboImpl server, ConnectedPlayer player) {
    this.plugin = plugin;
    this.server = server;
    this.player = player;

    this.connection = this.player.getConnection();
    this.sessionHandler = (LimboSessionHandlerImpl) this.connection.getActiveSessionHandler();
  }

  @Override
  public void writePacket(Object msg) {
    this.connection.delayedWrite(msg);
  }

  @Override
  public void writePacketAndFlush(Object msg) {
    this.connection.write(msg);
  }

  @Override
  public void flushPackets() {
    this.connection.flush();
  }

  @Override
  public void closeWith(Object msg) {
    this.connection.closeWith(msg);
  }

  @Override
  public ScheduledExecutorService getScheduledExecutor() {
    return this.connection.eventLoop();
  }

  @Override
  public void sendImage(int mapId, BufferedImage image, boolean sendItem, int itemSlot, boolean resize) {
    ProtocolVersion version = this.player.getProtocolVersion();
    if (sendItem) {
      // TODO check 1.16.5 and 1.20.6
      this.setItem(
          itemSlot,
          SimpleItem.fromItem(Item.FILLED_MAP),
          1,
          (short) mapId,
          version.noGreaterThan(ProtocolVersion.MINECRAFT_1_16_4) ? null : CompoundBinaryTag.builder().put("map", IntBinaryTag.intBinaryTag(mapId)).build(),
          null
      );
    }

    if (image.getWidth() != MapData.MAP_DIM_SIZE || image.getHeight() != MapData.MAP_DIM_SIZE) {
      if (resize) {
        BufferedImage resizedImage = new BufferedImage(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, image.getType());

        Graphics2D graphics = resizedImage.createGraphics();
        graphics.drawImage(image.getScaledInstance(MapData.MAP_DIM_SIZE, MapData.MAP_DIM_SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();

        image = resizedImage;
      } else {
        throw new IllegalStateException(
            "You either need to provide an image of " + MapData.MAP_DIM_SIZE + "x" + MapData.MAP_DIM_SIZE + " pixels or set the resize parameter to true so that API will automatically resize your image"
        );
      }
    }

    int[] toWrite = MapPalette.imageToBytes(image, version);
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      byte[][] canvas = new byte[MapData.MAP_DIM_SIZE][MapData.MAP_DIM_SIZE];
      for (int i = 0; i < MapData.MAP_SIZE; ++i) {
        canvas[i & 0x7F][i >> 7] = (byte) toWrite[i];
      }

      for (int i = 0; i < MapData.MAP_DIM_SIZE; ++i) {
        this.writePacket(new MapDataPacket(mapId, (byte) 0, new MapData(i, canvas[i])));
      }

      this.flushPackets();
    } else {
      byte[] canvas = new byte[MapData.MAP_SIZE];
      for (int i = 0; i < MapData.MAP_SIZE; ++i) {
        canvas[i] = (byte) toWrite[i];
      }

      this.writePacketAndFlush(new MapDataPacket(mapId, (byte) 0, new MapData(canvas)));
    }
  }

  @Override
  public void setItemInMainHand(VirtualItem item, int count) {
    this.writePacketAndFlush(new SetSlotPacket(0, 36, item, count));
  }

  @Override
  public void setItemInOffHand(VirtualItem item, int count) {
    this.writePacketAndFlush(new SetSlotPacket(0, 45, item, count));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count, CompoundBinaryTag nbt) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, nbt));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count, ItemComponentMap map) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, map));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count, short data) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, data));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count, short data, @Nullable CompoundBinaryTag nbt) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, data, nbt));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count, short data, @Nullable ItemComponentMap map) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, data, map));
  }

  @Override
  public void setItem(int slot, VirtualItem item, int count, short data, CompoundBinaryTag nbt, ItemComponentMap map) {
    this.writePacketAndFlush(new SetSlotPacket(0, slot, item, count, data, nbt, map));
  }

  @Override
  public void setGameMode(GameMode gameMode) {
    ProtocolVersion version = this.player.getProtocolVersion();
    boolean is17 = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6);
    if (gameMode != GameMode.SPECTATOR || !is17) { // Spectator game mode was added in 1.8
      this.gameMode = gameMode;
      int id = gameMode.getId();
      this.sendGameModeSpecificAbilities();
      if (!is17) {
        UUID uuid = LimboAPI.getClientUniqueId(this.player);
        if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_19_1)) {
          this.writePacket(new LegacyPlayerListItemPacket(LegacyPlayerListItemPacket.UPDATE_GAMEMODE, List.of(new LegacyPlayerListItemPacket.Item(uuid).setGameMode(id))));
        } else {
          UpsertPlayerInfoPacket.Entry playerInfoEntry = new UpsertPlayerInfoPacket.Entry(uuid);
          playerInfoEntry.setGameMode(id);
          this.writePacket(new UpsertPlayerInfoPacket(EnumSet.of(UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE), List.of(playerInfoEntry)));
        }
      }
      this.writePacket(new GameEventPacket(3, id)); // CHANGE_GAME_MODE
      this.flushPackets();
    }
  }

  @Override
  public void setWorldTime(long ticks) {
    this.writePacketAndFlush(new SetTimePacket(ticks, ticks));
  }

  @Override
  public void teleport(double posX, double posY, double posZ, float yaw, float pitch) {
    this.writePacketAndFlush(new PlayerPositionPacket(posX, posY, posZ, yaw, pitch, false, 44, true));
  }

  @Override
  public void disableFalling() {
    this.writePacketAndFlush(new PlayerAbilitiesPacket((byte) (this.getGameModeSpecificAbilities() | AbilityFlags.FLYING | AbilityFlags.ALLOW_FLYING), 0F, 0F));
  }

  @Override
  public void enableFalling() {
    this.writePacketAndFlush(new PlayerAbilitiesPacket((byte) (this.getGameModeSpecificAbilities() & (~AbilityFlags.FLYING)), 0.05F, 0.1F));
  }

  @Override
  public void disconnect() {
    this.connection.eventLoop().execute(() -> {
      if (this.connection.getActiveSessionHandler() == this.sessionHandler) {
        this.sessionHandler.disconnect(() -> {
          if (this.plugin.hasLoginQueue(this.player)) {
            if (this.player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
              this.sessionHandler.disconnectToConfig(() -> this.plugin.getLoginQueue(this.player).next());
            } else {
              this.sessionHandler.disconnected();
              this.plugin.getLoginQueue(this.player).next();
            }
          } else {
            RegisteredServer server = this.sessionHandler.getPreviousServer();
            if (server != null) {
              this.sendToRegisteredServer(server);
            } else {
              this.sessionHandler.disconnected();
            }
          }
        });
      }
    });
  }

  @Override
  public void disconnect(RegisteredServer server) {
    this.connection.eventLoop().execute(() -> {
      if (this.connection.getActiveSessionHandler() == this.sessionHandler) {
        this.sessionHandler.disconnect(() -> {
          if (this.plugin.hasLoginQueue(this.player)) {
            if (this.player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
              this.sessionHandler.disconnectToConfig(() -> {
                this.plugin.setNextServer(this.player, server);
                this.plugin.getLoginQueue(this.player).next();
              });
            } else {
              this.sessionHandler.disconnected();
              this.plugin.setNextServer(this.player, server);
              this.plugin.getLoginQueue(this.player).next();
            }
          } else {
            this.sendToRegisteredServer(server);
          }
        });
      }
    });
  }

  private void deject() {
    var pipeline = this.connection.getChannel().pipeline();
    this.plugin.deject3rdParty(pipeline);
    this.plugin.fixCompressor(pipeline, this.player.getProtocolVersion());
  }

  private void sendToRegisteredServer(RegisteredServer server) {
    this.deject();
    this.connection.setState(StateRegistry.PLAY);

    ProtocolVersion version = this.player.getProtocolVersion();
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      this.sessionHandler.disconnectToConfig(() -> {
        // Rollback original CONFIG handler
        this.connection.setActiveSessionHandler(StateRegistry.CONFIG, new ClientConfigSessionHandler(this.plugin.getServer(), this.player));
        this.player.createConnectionRequest(server).fireAndForget();
      });
    } else {
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        this.connection.delayedWrite(new LegacyPlayerListItemPacket(
            LegacyPlayerListItemPacket.REMOVE_PLAYER,
            List.of(new LegacyPlayerListItemPacket.Item(LimboAPI.getClientUniqueId(this.player)))
        ));
      }

      this.sessionHandler.disconnected();
      this.player.createConnectionRequest(server).fireAndForget();
    }
  }

  @Override
  public void setEntityData(int id, Collection<EntityDataValue<?>> packedItems) {
    this.writePacketAndFlush(new SetEntityDataPacket(id, packedItems));
  }

  @Override
  public void sendGameModeSpecificAbilities() {
    this.writePacketAndFlush(new PlayerAbilitiesPacket(this.getGameModeSpecificAbilities(), 0.05F, 0.1F));
  }

  @Override
  public void sendAbilities(int abilities, float flyingSpeed, float walkingSpeed) {
    this.writePacketAndFlush(new PlayerAbilitiesPacket((byte) abilities, flyingSpeed, walkingSpeed));
  }

  @Override
  public void sendAbilities(byte abilities, float flyingSpeed, float walkingSpeed) {
    this.writePacketAndFlush(new PlayerAbilitiesPacket(abilities, flyingSpeed, walkingSpeed));
  }

  @Override
  public byte getGameModeSpecificAbilities() {
    return switch (this.gameMode) {
      case CREATIVE -> AbilityFlags.ALLOW_FLYING | AbilityFlags.CREATIVE_MODE | AbilityFlags.INVULNERABLE;
      case SPECTATOR -> AbilityFlags.ALLOW_FLYING | AbilityFlags.INVULNERABLE | AbilityFlags.FLYING;
      default -> 0;
    };
  }

  @Override
  public GameMode getGameMode() {
    return this.gameMode;
  }

  @Override
  public Limbo getServer() {
    return this.server;
  }

  @Override
  public Player getProxyPlayer() {
    return this.player;
  }

  @Override
  public int getPing() {
    LimboSessionHandlerImpl handler = (LimboSessionHandlerImpl) this.connection.getActiveSessionHandler();
    return handler == null ? -1 : handler.getPing();
  }
}
