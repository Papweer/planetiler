package com.onthegomap.planetiler.examples.parsers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultsParser {

  private static final HashMap<String, Object> defaultsMap = new HashMap<>();

  public DefaultsParser() {
    initDefaultsList();
  }

  private static void initDefaultsList() {
    String filename = "/defaultValues.json";

    try (InputStream inputStream = DefaultsParser.class.getResourceAsStream(filename)) {
      if (inputStream == null) {
        throw new RuntimeException("Could not find " + filename + " in classpath.");
      }

      Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      JsonElement root = JsonParser.parseReader(reader);

      flatten(root, "", defaultsMap);

    } catch (Exception e) {
      throw new RuntimeException("Failed to load default values", e);
    }
  }

  // Recursive Flattening Logic
  private static void flatten(JsonElement element, String prefix, Map<String, Object> map) {
    if (element.isJsonObject()) {
      for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
        String newKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
        flatten(entry.getValue(), newKey, map);
      }
    } else if (element.isJsonPrimitive()) {
      map.put(prefix, element);
    }
  }

  private Optional<JsonElement> getElement(String path) {
    JsonElement element = (JsonElement) defaultsMap.get(path);
    return (element == null || element.isJsonNull()) ? Optional.empty() : Optional.of(element);
  }

  public Optional<String> getString(String path) {
    return getElement(path).map(JsonElement::getAsString);
  }

  public Optional<Double> getDouble(String path) {
    return getElement(path).map(JsonElement::getAsDouble);
  }

  public Optional<Integer> getInteger(String path) {
    return getElement(path).map(JsonElement::getAsInt);
  }

  public Optional<Boolean> getBoolean(String path) {
    return getElement(path).map(JsonElement::getAsBoolean);
  }
}
