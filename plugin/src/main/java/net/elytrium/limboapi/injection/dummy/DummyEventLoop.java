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

package net.elytrium.limboapi.injection.dummy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("ConstantConditions")
@SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "This is dummy class.")
public class DummyEventLoop implements EventLoop {

  @Override
  public EventLoopGroup parent() {
    return null;
  }

  @Override
  public boolean inEventLoop() {
    return true;
  }

  @Override
  public boolean inEventLoop(Thread thread) {
    return true;
  }

  @Override
  public <V> Promise<V> newPromise() {
    return null;
  }

  @Override
  public <V> ProgressivePromise<V> newProgressivePromise() {
    return null;
  }

  @Override
  public <V> Future<V> newSucceededFuture(V v) {
    return null;
  }

  @Override
  public <V> Future<V> newFailedFuture(Throwable t) {
    return null;
  }

  @Override
  public boolean isShuttingDown() {
    return false;
  }

  @Override
  public Future<?> shutdownGracefully() {
    return null;
  }

  @Override
  public Future<?> shutdownGracefully(long l, long l1, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public Future<?> terminationFuture() {
    return null;
  }

  @Override
  public void shutdown() {

  }

  @Override
  public List<Runnable> shutdownNow() {
    return null;
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long l, @NonNull TimeUnit timeUnit) {
    return false;
  }

  @Override
  public EventLoop next() {
    return null;
  }

  @Override
  public Iterator<EventExecutor> iterator() {
    return null;
  }

  @Override
  public Future<?> submit(Runnable runnable) {
    return null;
  }

  @Override
  public <T> Future<T> submit(Runnable runnable, T t) {
    return null;
  }

  @Override
  public <T> Future<T> submit(Callable<T> callable) {
    return null;
  }

  @NonNull
  @Override
  public <T> List<java.util.concurrent.Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> collection) {
    return null;
  }

  @NonNull
  @Override
  public <T> List<java.util.concurrent.Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> collection, long l, @NonNull TimeUnit timeUnit) {
    return null;
  }

  @NonNull
  @Override
  public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> collection) {
    return null;
  }

  @Override
  public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> collection, long l, @NonNull TimeUnit timeUnit) {
    return null;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable runnable, long l, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long l, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public ChannelFuture register(Channel channel) {
    return null;
  }

  @Override
  public ChannelFuture register(ChannelPromise channelPromise) {
    return null;
  }

  @Override
  public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
    return null;
  }

  @Override
  public void execute(@NonNull Runnable runnable) {

  }
}
