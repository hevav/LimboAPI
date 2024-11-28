package net.elytrium.limboapi.utils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JsonParser {

  private static final Gson GSON = new Gson();
  private static final TypeToken<?> TOKEN = TypeToken.get(LinkedTreeMap.class);

  @SuppressWarnings("unchecked")
  public static <V> LinkedTreeMap<String, V> parse(InputStream inputStream) {
    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return (LinkedTreeMap<String, V>) JsonParser.GSON.fromJson(reader, JsonParser.TOKEN);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
