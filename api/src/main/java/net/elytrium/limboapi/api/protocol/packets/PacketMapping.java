/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PacketMapping {

  private final int id;
  private final ProtocolVersion protocolVersion;
  @Nullable
  private final ProtocolVersion lastValidProtocolVersion;
  private final boolean encodeOnly;

  public PacketMapping(int id, ProtocolVersion protocolVersion, boolean encodeOnly) {
    this(id, protocolVersion, null, encodeOnly);
  }

  public PacketMapping(int id, ProtocolVersion protocolVersion, @Nullable ProtocolVersion lastValidProtocolVersion, boolean encodeOnly) {
    this.id = id;
    this.protocolVersion = protocolVersion;
    this.lastValidProtocolVersion = lastValidProtocolVersion;
    this.encodeOnly = encodeOnly;
  }

  @Deprecated(forRemoval = true)
  public int getID() {
    return this.id;
  }

  public int getId() {
    return this.id;
  }

  public ProtocolVersion getProtocolVersion() {
    return this.protocolVersion;
  }

  @Nullable
  public ProtocolVersion getLastValidProtocolVersion() {
    return this.lastValidProtocolVersion;
  }

  public boolean isEncodeOnly() {
    return this.encodeOnly;
  }
}
