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

import java.util.List;
import net.elytrium.commons.config.YamlConfig;
import net.elytrium.commons.kyori.serialization.Serializers;

public class Settings extends YamlConfig {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = "BuildConstants.LIMBO_VERSION"; // TODO

  @Comment({
      "Available serializers:",
      "LEGACY_AMPERSAND - \"&c&lExample &c&9Text\".",
      "LEGACY_SECTION - \"§c§lExample §c§9Text\".",
      "MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)",
      "GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)",
      "GSON_COLOR_DOWNSAMPLING - Same as GSON, but uses downsampling."
  })
  public Serializers SERIALIZER = Serializers.LEGACY_AMPERSAND;
  public String PREFIX = "LimboAPI &6>>&f";

  @Create
  public MAIN MAIN;

  @Comment("Don't use \\n, use {NL} for new line, and {PRFX} for prefix.")
  public static class MAIN {

    public boolean CHECK_FOR_UPDATES = true;

    public int MAX_CHAT_MESSAGE_LENGTH = 256;
    public int MAX_BRAND_NAME_LENGTH = 64;
    public int MAX_UNKNOWN_PACKET_LENGTH = 2048;
    public int MAX_SINGLE_GENERIC_PACKET_LENGTH = 4096;
    public int MAX_MULTI_GENERIC_PACKET_LENGTH = 131072;
    @Comment({
        "Default max packet length (in bytes) that will be proceeded, other packets will be dropped.",
        "Can be increased with Limbo#setMaxSuppressPacketLength"
    })
    public int MAX_PACKET_LENGTH_TO_SUPPRESS_IT = 512;
    @Comment({
        "Discards all packets longer than compression-threshold. Helps to mitigate some attacks.",
        "Needs compression-threshold to be 300 or higher to support 1.19 chat-signing, so it is disabled by default"
    })
    public boolean DISCARD_COMPRESSION_ON_LOGIN = false;
    public boolean DISCARD_COMPRESSION_AFTER_LOGIN = false;
    @Comment({
        "LimboAPI will consume more RAM if this option is enabled, but compatibility with other plugins will be better",
        "Enable it if you have a plugin installed that bypasses compression (e.g. Geyser)"
    })
    public boolean SAVE_UNCOMPRESSED_PACKETS = true;
    @Comment({
        "WARNING: do not change when proxy is running, it will break exsiting connections",
        "LimboAPI will be running in compatibility mode, allowing other plugins to",
        "intercept or modify packets sent by it, but will reduce performance in some cases.",
        "Enable if you are using plugins that modify packets. (e.g. ViaVersion, Raknetify or PacketEvents)",
        "Require 'save-uncompressed-packets: true' to work properly"
    })
    public boolean COMPATIBILITY_MODE = false;

    @Comment("Logging for connect and disconnect messages.")
    public boolean LOGGING_ENABLED = true;
    @Comment({
        "Change the parameters below, if you want to reduce the RAM consumption.",
        "Use VelocityTools to completely block Minecraft versions (https://github.com/Elytrium/VelocityTools/releases/latest).",
        "Available versions:",
        "1_7_2, 1_7_6, 1_8, 1_9, 1_9_1, 1_9_2, 1_9_4, 1_10, 1_11, 1_11_1, 1_12, 1_12_1, 1_12_2,",
        "1_13, 1_13_1, 1_13_2, 1_14, 1_14_1, 1_14_2, 1_14_3, 1_14_4, 1_15, 1_15_1, 1_15_2,",
        "1_16, 1_16_1, 1_16_2, 1_16_3, 1_16_4, 1_17, 1_17_1, 1_18, 1_18_2, 1_19, 1_19_1, 1_19_3,",
        "1_20, 1_20_2, 1_20_3, 1_20_5, 1_21, LATEST"
    })
    public String PREPARE_MIN_VERSION = "1_7_2";
    public String PREPARE_MAX_VERSION = "LATEST";

    @Comment("Helpful if you want some plugins proceed before LimboAPI. For example, it is needed to Floodgate to replace UUID.")
    public List<String> PRE_LIMBO_PROFILE_REQUEST_PLUGINS = List.of("floodgate", "geyser");

    @Comment("Should reduced debug info be enabled (reduced information in F3) if there is no preference for Limbo")
    public boolean REDUCED_DEBUG_INFO = false;

    public int VIEW_DISTANCE = 10;

    public int SIMULATION_DISTANCE = 9;

    @Comment({
        "How many chunks we should send when a player spawns.",
        " 0 = send no chunks on spawn.",
        " 1 = send only the spawn chunk.",
        " 2 = send the spawn chunk and chunks next to the spawn chunk.",
        " 3 = send the spawn chunk, chunks next to the spawn chunk and next to these chunks.",
        " and so on.."
    })
    public int CHUNK_RADIUS_SEND_ON_SPAWN = 2;

    @Comment("How many chunks we should send per tick")
    public int CHUNKS_PER_TICK = 16;

    @Comment("Maximum delay for receiving ChatSession packet (for online-mode client-side race condition mitigation)")
    public int CHAT_SESSION_PACKET_TIMEOUT = 5000;

    @Comment("Ability to force disable chat signing on 1.19.3+")
    public boolean FORCE_DISABLE_MODERN_CHAT_SIGNING = true;

    @Create
    public MESSAGES MESSAGES;

    public static class MESSAGES {

      public String TOO_BIG_PACKET = "{PRFX}{NL}{NL}&cYour client sent too big packet!";
      public String INVALID_PING = "{PRFX}{NL}{NL}&cYour client sent invalid ping packet!";
      public String INVALID_SWITCH = "{PRFX}{NL}{NL}&cYour client sent an unexpected state switching packet!";
      public String TIME_OUT = "{PRFX}{NL}{NL}Timed out.";
    }
  }
}
