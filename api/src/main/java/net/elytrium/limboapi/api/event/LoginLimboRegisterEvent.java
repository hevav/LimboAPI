/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.event;

import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

/**
 * This event is fired during a login process before the player has been authenticated, e.g., to enable or disable custom authentication
 */
public class LoginLimboRegisterEvent {

  private final Player player;
  private final Queue<Runnable> onJoinCallbacks;

  private Function<KickedFromServerEvent, Boolean> onKickCallback;

  public LoginLimboRegisterEvent(Player player) {
    this.player = Objects.requireNonNull(player, "player");
    this.onJoinCallbacks = new LinkedBlockingQueue<>();
  }

  public Player getPlayer() {
    return this.player;
  }

  @Override
  public String toString() {
    return "LoginLimboRegisterEvent{"
        + "player=" + this.player
        + "}";
  }

  public Queue<Runnable> getOnJoinCallbacks() {
    return this.onJoinCallbacks;
  }

  public Function<KickedFromServerEvent, Boolean> getOnKickCallback() {
    return this.onKickCallback;
  }

  public void addOnJoinCallback(Runnable callback) {
    this.onJoinCallbacks.add(callback);
  }

  /**
   * @deprecated Use {@link LoginLimboRegisterEvent#addOnJoinCallback(Runnable)} instead
   */
  @Deprecated(forRemoval = true)
  public void addCallback(Runnable callback) {
    this.onJoinCallbacks.add(callback);
  }

  public void setOnKickCallback(Function<KickedFromServerEvent, Boolean> callback) {
    this.onKickCallback = callback;
  }
}
