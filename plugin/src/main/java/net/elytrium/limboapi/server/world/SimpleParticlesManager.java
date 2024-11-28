package net.elytrium.limboapi.server.world;

import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.utils.JsonParser;

// TODO legacy (<=1.12.2) + expose into api
public class SimpleParticlesManager {

  private static final EnumMap<ProtocolVersion, Map<Integer, Particle>> PROTOCOL_2_PARTICLE = new EnumMap<>(ProtocolVersion.class);

  static {
    LinkedTreeMap<String, LinkedTreeMap<String, Number>> particlesMappings = JsonParser.parse(LimboAPI.class.getResourceAsStream("/mappings/particle_types_mappings.json"));
    particlesMappings.forEach((modernId, versions) -> {
      Particle particle = Particle.valueOf(modernId.substring(10/*"minecraft:".length()*/).toUpperCase(Locale.US));
      versions.forEach((version, id) -> SimpleParticlesManager.PROTOCOL_2_PARTICLE.computeIfAbsent(
          ProtocolVersion.getProtocolVersion(Integer.parseInt(version)),
          key -> new HashMap<>(versions.size())
      ).put(id.intValue(), particle));
    });
  }

  public static Particle fromProtocolId(ProtocolVersion version, int id) {
    return SimpleParticlesManager.PROTOCOL_2_PARTICLE.get(version).get(id);
  }
}
