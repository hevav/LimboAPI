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

package net.elytrium.limboapi.injection.event;

import com.google.common.collect.ListMultimap;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.event.VelocityEventManager;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.Settings;
import net.elytrium.limboapi.utils.Reflection;

@SuppressWarnings("unchecked")
public class EventManagerHook {

  private static final MethodHandle FIRE_METHOD = Reflection.findVirtualVoid(
      VelocityEventManager.class,
      "fire",
      CompletableFuture.class, Object.class, int.class, boolean.class, Reflection.findClass("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration").arrayType()
  );
  private static final MethodHandle FUTURE_FIELD = Reflection.findGetter(Reflection.findClass("com.velocitypowered.proxy.event.VelocityEventManager$ContinuationTask"), "future", CompletableFuture.class);

  private final Set<GameProfile> proceededProfiles = new HashSet<>();
  private final LimboAPI plugin;
  private final VelocityEventManager eventManager;

  private Object handlerRegistrations;
  private boolean hasHandlerRegistration;

  public EventManagerHook(LimboAPI plugin, VelocityEventManager eventManager) {
    this.plugin = plugin;
    this.eventManager = eventManager;
  }

  @Subscribe(order = PostOrder.FIRST)
  public EventTask onGameProfileRequest(GameProfileRequestEvent event) {
    if (this.proceededProfiles.remove(event.getGameProfile())) {
      return null;
    } else {
      CompletableFuture<GameProfileRequestEvent> fireFuture = new CompletableFuture<>();
      CompletableFuture<GameProfileRequestEvent> hookFuture = new CompletableFuture<>();
      fireFuture.thenAccept(modifiedEvent -> {
        try {
          this.plugin.getLoginListener().hookLoginSession(modifiedEvent);
          hookFuture.complete(modifiedEvent);
        } catch (Throwable t) {
          throw new ReflectionException(t);
        }
      });

      if (this.hasHandlerRegistration) {
        try {
          EventManagerHook.FIRE_METHOD.invoke(this.eventManager, fireFuture, event, 0, false/*currentlyAsync, passing false to run continuation tasks in asyncExecutor*/, this.handlerRegistrations);
        } catch (Throwable t) {
          fireFuture.complete(event);
          throw new ReflectionException(t);
        }
      } else {
        fireFuture.complete(event);
      }

      // ignoring other subscribers by directly completing the future
      return EventTask.withContinuation(continuation -> hookFuture.whenComplete((result, cause) -> {
        try {
          CompletableFuture<GameProfileRequestEvent> future = (CompletableFuture<GameProfileRequestEvent>) FUTURE_FIELD.invokeExact(continuation);
          if (future != null) {
            future.complete(result);
          }
        } catch (Throwable t) {
          throw new ReflectionException(t);
        }
      }));
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public EventTask onKickedFromServer(KickedFromServerEvent event) {
    CompletableFuture<KickedFromServerEvent> hookFuture = new CompletableFuture<>();
    try {
      Function<KickedFromServerEvent, Boolean> callback = this.plugin.getKickCallback(event.getPlayer());
      if (callback == null || !callback.apply(event)) {
        hookFuture.complete(event);
      }
    } catch (Throwable t) {
      LimboAPI.getLogger().error("Failed to handle KickCallback, ignoring its result", t);
      hookFuture.complete(event);
    }

    // if kick callback is null and no exception occurred, hookFuture won't be ever finished, and
    // the event chain would be broken, that is what we need.
    return EventTask.resumeWhenComplete(hookFuture);
  }

  public void proceedProfile(GameProfile profile) {
    this.proceededProfiles.add(profile);
  }

  @SuppressWarnings("rawtypes")
  public void reloadHandlers() throws Throwable {
    Field handlersByTypeField = VelocityEventManager.class.getDeclaredField("handlersByType");
    handlersByTypeField.setAccessible(true);
    ListMultimap<Class<?>, ?> handlersByType = (ListMultimap<Class<?>, ?>) handlersByTypeField.get(this.eventManager);

    List disabledHandlers = handlersByType.get(GameProfileRequestEvent.class);
    List preEvents = new ArrayList<>();
    List newHandlers = new ArrayList<>(disabledHandlers);

    if (this.handlerRegistrations != null) {
      for (int i = 0; i < Array.getLength(this.handlerRegistrations); ++i) {
        preEvents.add(Array.get(this.handlerRegistrations, i));
      }
    }

    Class<?> HandlerRegistration = Reflection.findClass("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration");
    Field pluginField = HandlerRegistration.getDeclaredField("plugin");
    pluginField.setAccessible(true);
    for (Object handler : disabledHandlers) {
      String id = ((PluginContainer) pluginField.get(handler)).getDescription().getId();
      if (Settings.IMP.MAIN.PRE_LIMBO_PROFILE_REQUEST_PLUGINS.contains(id)) {
        LimboAPI.getLogger().info("Hooking all GameProfileRequestEvent events from {}", id);
        preEvents.add(handler);
        newHandlers.remove(handler);
      }
    }

    handlersByType.replaceValues(GameProfileRequestEvent.class, newHandlers);
    this.handlerRegistrations = Array.newInstance(HandlerRegistration, preEvents.size());

    for (int i = 0; i < preEvents.size(); ++i) {
      Array.set(this.handlerRegistrations, i, preEvents.get(i));
    }

    this.hasHandlerRegistration = !preEvents.isEmpty();
  }
}
