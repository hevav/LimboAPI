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

package net.elytrium.limboapi.injection.tablist;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import java.util.UUID;
import net.elytrium.limboapi.LimboAPI;

public interface RewritingTabList {

  Player getPlayer();

  default TabListEntry rewriteEntry(TabListEntry entry) {
    GameProfile profile;
    UUID profileId;
    if (entry == null || (profile = entry.getProfile()) == null || !this.getPlayer().getUniqueId().equals(profileId = profile.getId())) {
      return entry;
    }

    return TabListEntry.builder()
        .tabList(entry.getTabList())
        .profile(new GameProfile(this.rewriteUuid(profileId), profile.getName(), profile.getProperties()))
        .chatSession(entry.getChatSession())
        .displayName(entry.getDisplayNameComponent().orElse(null))
        .latency(entry.getLatency())
        .gameMode(entry.getGameMode())
        .listed(entry.isListed())
        .listOrder(entry.getListOrder())
        .build();
  }

  default UUID rewriteUuid(UUID uuid) {
    Player player = this.getPlayer();
    if (player.getUniqueId().equals(uuid)) {
      UUID clientUniqueId = LimboAPI.getClientUniqueId(player);
      return clientUniqueId == null ? uuid : clientUniqueId;
    }

    return uuid;
  }
}
