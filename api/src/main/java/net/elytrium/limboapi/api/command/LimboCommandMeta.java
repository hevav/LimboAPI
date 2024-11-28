/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import java.util.Collection;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Deprecated(forRemoval = true)
public class LimboCommandMeta implements CommandMeta {

  @NonNull
  private final Collection<String> aliases;
  @NonNull
  private final Collection<CommandNode<CommandSource>> hints;
  @Nullable
  private final Object plugin;

  public LimboCommandMeta(@NonNull Collection<String> aliases) {
    this(aliases, null, null);
  }

  public LimboCommandMeta(@NonNull Collection<String> aliases, @Nullable Collection<CommandNode<CommandSource>> hints) {
    this(aliases, hints, null);
  }

  public LimboCommandMeta(@NonNull Collection<String> aliases, @Nullable Collection<CommandNode<CommandSource>> hints, @Nullable Object plugin) {
    this.aliases = aliases;
    this.hints = Objects.requireNonNullElse(hints, ImmutableList.of());
    this.plugin = plugin;
  }

  @NonNull
  @Override
  public Collection<String> getAliases() {
    return this.aliases;
  }

  @NonNull
  @Override
  public Collection<CommandNode<CommandSource>> getHints() {
    return this.hints;
  }

  @Nullable
  @Override
  public Object getPlugin() {
    return this.plugin;
  }
}
