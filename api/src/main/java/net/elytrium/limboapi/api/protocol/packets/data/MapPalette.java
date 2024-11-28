/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * The LimboAPI (excluding the LimboAPI plugin) is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package net.elytrium.limboapi.api.protocol.packets.data;

import com.velocitypowered.api.network.ProtocolVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.image.BufferedImage;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@SuppressFBWarnings("MS_EXPOSE_REP")
public class MapPalette {

  private static final Map<MapVersion, byte[]> REMAP_BUFFERS = new EnumMap<>(MapVersion.class);
  private static final byte[] MAIN_BUFFER = readBuffer("/mappings/colors_main_map");

  /**
   * @deprecated Use {@link java.awt.Color#WHITE} instead
   */
  @Deprecated
  public static final byte WHITE = 34;
  public static final byte TRANSPARENT = 0;

  static {
    for (MapVersion version : MapVersion.values()) {
      REMAP_BUFFERS.put(version, readBuffer("/mappings/colors_" + version.toString().toLowerCase(Locale.ROOT) + "_map"));
    }
  }

  private static byte[] readBuffer(String filename) {
    try (InputStream stream = MapPalette.class.getResourceAsStream(filename)) {
      return Objects.requireNonNull(stream).readAllBytes();
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  /**
   * Convert an Image to a byte[] using the palette
   * Uses a reduced set of colors, to support more colors use {@link MapPalette#imageToBytes(BufferedImage, ProtocolVersion)}
   *
   * @param image The image to convert
   *
   * @return A byte[] containing the pixels of the image
   */
  public static int[] imageToBytes(BufferedImage image) {
    return imageToBytes(image, ProtocolVersion.MINIMUM_VERSION);
  }

  /**
   * Convert an Image to a byte[] using the palette
   *
   * @param image   The image to convert
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static int[] imageToBytes(BufferedImage image, ProtocolVersion version) {
    int[] result = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    return imageToBytes(result, result, version);
  }

  /**
   * Convert an image to a byte[] using the palette
   *
   * @param image   The image to convert
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static int[] imageToBytes(int[] image, ProtocolVersion version) {
    return imageToBytes(image, new int[image.length], version);
  }

  /**
   * Convert an image to a byte[] using the palette
   *
   * @param from    The image to convert
   * @param to      Output image
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static int[] imageToBytes(int[] from, int[] to, ProtocolVersion version) {
    for (int i = 0; i < from.length; ++i) {
      to[i] = tryFastMatchColor(from[i], version);
    }

    return to;
  }

  /**
   * Convert an image to a byte[] using the palette
   *
   * @param from    The image to convert
   * @param to      Output image
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static byte[] imageToBytes(int[] from, byte[] to, ProtocolVersion version) {
    for (int i = 0; i < from.length; ++i) {
      to[i] = tryFastMatchColor(from[i], version);
    }

    return to;
  }

  /**
   * Get the index of the closest matching color in the palette to the given color
   * Uses caching and downscaling of color values
   *
   * @param rgb The Color to match
   *
   * @return The index in the palette
   */
  public static byte tryFastMatchColor(int rgb, ProtocolVersion version) {
    if (getAlpha(rgb) < 128) {
      return TRANSPARENT;
    } else {
      MapVersion mapVersion = MapVersion.fromProtocolVersion(version);
      byte originalColorId = MAIN_BUFFER[rgb & 0xFFFFFF];
      if (mapVersion == MapVersion.MAXIMUM_VERSION) {
        return originalColorId;
      } else {
        return remapByte(REMAP_BUFFERS.get(mapVersion), originalColorId);
      }
    }
  }

  private static int getAlpha(int rgb) {
    return (rgb & 0xFF000000) >>> 24;
  }

  /**
   * Convert an image from MapVersion.MAXIMUM_VERSION to the desired version
   *
   * @param image   The image to convert
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static int[] convertImage(int[] image, MapVersion version) {
    return convertImage(image, new int[image.length], version);
  }

  /**
   * Convert an image from MapVersion.MAXIMUM_VERSION to the desired version
   *
   * @param from    The image to convert
   * @param to      Output image
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static int[] convertImage(int[] from, int[] to, MapVersion version) {
    byte[] remapBuffer = REMAP_BUFFERS.get(version);
    for (int i = 0; i < from.length; ++i) {
      to[i] = remapByte(remapBuffer, (byte) from[i]);
    }

    return to;
  }

  /**
   * Convert an image from MapVersion.MAXIMUM_VERSION to the desired version
   *
   * @param from    The image to convert
   * @param to      Output image
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static byte[] convertImage(byte[] from, byte[] to, MapVersion version) {
    byte[] remapBuffer = REMAP_BUFFERS.get(version);
    for (int i = 0; i < from.length; ++i) {
      to[i] = remapByte(remapBuffer, from[i]);
    }

    return to;
  }

  /**
   * Convert an image from MapVersion.MAXIMUM_VERSION to the desired version
   *
   * @param from    The image to convert
   * @param to      Output image
   * @param version The ProtocolVersion to support more colors
   *
   * @return A byte[] containing the pixels of the image
   */
  public static byte[] convertImage(int[] from, byte[] to, MapVersion version) {
    byte[] remapBuffer = REMAP_BUFFERS.get(version);
    for (int i = 0; i < from.length; ++i) {
      to[i] = remapByte(remapBuffer, (byte) from[i]);
    }

    return to;
  }

  private static byte remapByte(byte[] remapBuffer, byte oldByte) {
    return remapBuffer[oldByte & 0xFF];
  }

  public enum MapVersion {

    MINIMUM_VERSION(EnumSet.range(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_7_6)),
    MINECRAFT_1_8(EnumSet.range(ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_11_1)),
    MINECRAFT_1_12(EnumSet.range(ProtocolVersion.MINECRAFT_1_12, ProtocolVersion.MINECRAFT_1_15_2)),
    MINECRAFT_1_16(EnumSet.range(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MINECRAFT_1_16_4)),
    MINECRAFT_1_17(EnumSet.range(ProtocolVersion.MINECRAFT_1_17, ProtocolVersion.MAXIMUM_VERSION));

    private static final EnumMap<ProtocolVersion, MapVersion> VERSIONS_MAP = new EnumMap<>(ProtocolVersion.class);

    public static final MapVersion MAXIMUM_VERSION = MINECRAFT_1_17;

    private final EnumSet<ProtocolVersion> versions;

    MapVersion(EnumSet<ProtocolVersion> versions) {
      this.versions = versions;
    }

    public EnumSet<ProtocolVersion> getVersions() {
      return this.versions;
    }

    static {
      for (MapVersion value : MapVersion.values()) {
        value.versions.forEach(version -> VERSIONS_MAP.put(version, value));
      }
    }

    public static MapVersion fromProtocolVersion(ProtocolVersion version) {
      return VERSIONS_MAP.get(version);
    }
  }
}
