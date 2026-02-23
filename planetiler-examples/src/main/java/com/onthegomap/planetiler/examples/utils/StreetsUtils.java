package com.onthegomap.planetiler.examples.utils;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.examples.parsers.ColorParser;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.examples.parsers.DirectionParser;
import com.onthegomap.planetiler.examples.parsers.TypeParser;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmSourceFeature;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


public class StreetsUtils {
  private static final ColorParser colorParser = new ColorParser();
  private static final DefaultsParser defaultsParser = new DefaultsParser();

  private static final List<String> memorialTypes = Arrays.asList(
    "war_memorial", "stele", "obelisk", "memorial", "stone"
  );

  public static Optional<String> getOptionalTag(SourceFeature sourceFeature, String tag) {
    return Optional.ofNullable((String) sourceFeature.getTag(tag));
  }
  public static Optional<Integer> getOptionalIntTag(SourceFeature sourceFeature, String tag) {
    return Optional.ofNullable(TypeParser.parseInt((String) sourceFeature.getTag(tag)));
  }

  public static Optional<Integer> getOptionalUnsignedIntTag(SourceFeature sourceFeature, String tag) {
    return Optional.ofNullable(TypeParser.parseUnsignedInt((String) sourceFeature.getTag(tag)));
  }

  public static Optional<Boolean> getOptionalBoolTag(SourceFeature sourceFeature, String tag) {
      return Optional.ofNullable(TypeParser.parseBool((String) sourceFeature.getTag(tag)));
  }

  public static Boolean orNullBooleans(Boolean... values) {
    Boolean result = null;
    for (Boolean v : values) {
      if (Boolean.TRUE.equals(v)) return true;  // can't get better, short-circuit
      if (Boolean.FALSE.equals(v)) result = false; // upgrade from null, but keep looking
    }
    return result;
  }

  public static boolean isMemorial(SourceFeature sourceFeature) {
    String memorialType = (String) sourceFeature.getTag("memorial");

    return sourceFeature.hasTag("historic", "memorial") && (
      memorialTypes.contains(memorialType) || memorialType == null
    );
  }

  public static boolean isFireHydrant(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("emergency", "fire_hydrant") &&
      (sourceFeature.hasTag("fire_hydrant:type", "pillar") || sourceFeature.getTag("fire_hydrant:type") == null);
  }

  public static boolean isStatue(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("historic", "memorial") && sourceFeature.hasTag("memorial", "statue") ||
      sourceFeature.hasTag("tourism", "artwork") && sourceFeature.hasTag("artwork_type", "statue");
  }

  public static boolean isSculpture(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("tourism", "artwork") && sourceFeature.hasTag("artwork_type", "sculpture") ||
      sourceFeature.hasTag("historic", "memorial") && sourceFeature.hasTag("memorial", "sculpture");
  }

  public static boolean isWindTurbine(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("power", "generator") && sourceFeature.hasTag("generator:source", "wind");
  }

  public static boolean isRailway(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("railway",
      "rail",
      "light_rail",
      "subway",
      "disused",
      "narrow_gauge",
      "tram"
    );
  }

  public static boolean isWater(SourceFeature sourceFeature) {
    return sourceFeature.getSource().equals("water") ||
      sourceFeature.hasTag("natural", "water") ||
      (
        sourceFeature.hasTag("leisure", "swimming_pool") &&
        !sourceFeature.hasTag("location", "indoor", "roof")
      );
  }

  public static String getFenceType(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("fence_type"));
  }

  public static String getWallType(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("wall"));
  }

  public static String getRailwayType(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("railway"));
  }

  public static String getWaterwayType(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("waterway"));
  }

  public static Double getTreeHeight(SourceFeature sourceFeature) {
    return getHeight(sourceFeature);

    /*Double height = getHeight(sourceFeature);
    if (height != null) {
      return height;
    }
    // We need minHeight in case tree height is implied from tags other than "height"
    Double minHeight = getMinHeight(sourceFeature);
    if (minHeight == null) minHeight = 0d;
    Double width = parseMeters((String) sourceFeature.getTag("diameter_crown"));
    if (width != null) {
      return width * 2 + minHeight;
    }
    // Diameter is in millimeters if no unit of measurement is specified
    Double diameter = parseMillimeters((String) sourceFeature.getTag("diameter"));
    if (diameter != null) {
      return diameter * 60 + minHeight;
    }
    Double circumference = parseMeters((String) sourceFeature.getTag("circumference"));
    if (circumference != null) {
      return circumference / Math.PI * 60 + minHeight;
    }
    return null;*/
  }

  public static Double getHeight(SourceFeature sourceFeature) {
    String height = (String) sourceFeature.getTag("height");
    String estHeight = (String) sourceFeature.getTag("est_height");

    if (height != null) {
      return TypeParser.parseMeters(height);
    }

    return TypeParser.parseMeters(estHeight);
  }

  public static Double getMinHeight(SourceFeature sourceFeature) {
    return TypeParser.parseMeters((String) sourceFeature.getTag("min_height"));
  }

  public static Double getRoofHeight(SourceFeature sourceFeature) {
    return TypeParser.parseMeters((String) sourceFeature.getTag("roof:height"));
  }

  public static Integer getRoofLevels(SourceFeature sourceFeature) {
    return TypeParser.parseUnsignedInt((String) sourceFeature.getTag("roof:levels"));
  }

  public static String getRoofMaterial(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("roof:material"));
  }

  public static String getRoofShape(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("roof:shape"));
  }

  public static String getBuildingMaterial(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("building:material"));
  }

  public static Integer getBuildingLevels(SourceFeature sourceFeature) {
    return TypeParser.parseUnsignedInt((String) sourceFeature.getTag("building:levels"));
  }

  public static Integer getBuildingMinLevel(SourceFeature sourceFeature) {
    return TypeParser.parseUnsignedInt((String) sourceFeature.getTag("building:min_level"));
  }

  public static Integer getBuildingColor(SourceFeature sourceFeature) {
    String color = getFirstTagValue((String) sourceFeature.getTag("building:colour"));
    return colorParser.parseColor(color);
  }

  public static Integer getRoofColor(SourceFeature sourceFeature) {
    String color = getFirstTagValue((String) sourceFeature.getTag("roof:colour"));
    return colorParser.parseColor(color);
  }

  public static String getRoofOrientation(SourceFeature sourceFeature) {
    String orientation = (String) sourceFeature.getTag("roof:orientation", "");

    if (orientation.equals("along") || orientation.equals("across")) {
      return orientation;
    }

    return null;
  }

  public static Optional<Double> getWidth(SourceFeature sourceFeature) {
    Double width = TypeParser.parseUnits((String) sourceFeature.getTag("width"), 1.0);
    if (width == null) {
      width = TypeParser.parseUnits((String) sourceFeature.getTag("est_width"), 1.0);
    }
    return Optional.ofNullable(width);
  }

  public static Double getDirection(SourceFeature sourceFeature) {
    return DirectionParser.parse((String) sourceFeature.getTag("direction"));
  }

  public static Double getRoofDirection(SourceFeature sourceFeature) {
    return DirectionParser.parse((String) sourceFeature.getTag("roof:direction"));
  }

  public static Double getAngle(SourceFeature sourceFeature) {
    return TypeParser.parseDouble((String) sourceFeature.getTag("angle"));
  }

  public static String getLeafType(SourceFeature sourceFeature) {
    String leafType = (String) sourceFeature.getTag("leaf_type");

    return getFirstTagValue(leafType);
  }

  public static String getGenus(SourceFeature sourceFeature) {
    String genusValue = getFirstTagValue((String) sourceFeature.getTag("genus"));
    String genusEngValue = getFirstTagValue((String) sourceFeature.getTag("genus:en"));

    return genusValue != null ? genusValue : genusEngValue;
  }

  public static Optional<String> getSurface(SourceFeature sourceFeature) {
    return Optional.ofNullable(getFirstTagValue((String) sourceFeature.getTag("surface")));
  }

  public static String getGauge(SourceFeature sourceFeature) {
    return (String) sourceFeature.getTag("gauge");
  }

  public static String getFlagWikidata(SourceFeature sourceFeature) {
    String wikidata0 = getFirstTagValue((String) sourceFeature.getTag("flag:wikidata"));
    String wikidata1 = getFirstTagValue((String) sourceFeature.getTag("subject:wikidata"));

    return wikidata0 != null ? wikidata0 : wikidata1;
  }

  public static String getFlagCountry(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("country"));
  }

  public static String getLampSupport(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("support"));
  }

  public static String getCrop(SourceFeature sourceFeature) {
    return getFirstTagValue((String) sourceFeature.getTag("crop"));
  }

  public static String getFirstTagValue(String value) {
    if (value == null) {
      return null;
    }

    String[] values = value.split(";");

    if (values.length == 0) {
      return null;
    }

    return values[0].trim().toLowerCase();
  }

  public static boolean isUnderground(SourceFeature sourceFeature) {
    Double layer = TypeParser.parseDouble((String) sourceFeature.getTag("layer"));

    if (layer != null && layer < 0) {
      return true;
    }

    String tunnelValue = (String)sourceFeature.getTag("tunnel", "no");
    boolean isInTunnel = !tunnelValue.equals("no");

    return sourceFeature.hasTag("location", "underground") ||
      isInTunnel ||
      sourceFeature.hasTag("parking", "underground");
  }

  public static Boolean getBuildingWindows(SourceFeature sourceFeature) {
    String windowValue = (String)sourceFeature.getTag("window", "");
    String windowsValue = (String)sourceFeature.getTag("windows", "");

    if (windowValue.equals("no") || windowsValue.equals("no")) {
      return false;
    }

    if (windowValue.equals("yes") || windowsValue.equals("yes")) {
      return true;
    }

    if (
      sourceFeature.hasTag("bridge:support") ||
        sourceFeature.hasTag("man_made", "storage_tank") ||
        sourceFeature.hasTag("man_made", "chimney") ||
        sourceFeature.hasTag("man_made", "stele") ||
        sourceFeature.hasTag("advertising", "billboard") ||
        sourceFeature.hasTag("historic", "city_gate") ||
        sourceFeature.hasTag("memorial", "statue")
    ) {
      return false;
    }

    return null;
  }

  public static Boolean getBuildingDefaultRoof(SourceFeature sourceFeature) {
    if (
      sourceFeature.hasTag("bridge:support") ||
      sourceFeature.hasTag("ship:type") ||
      sourceFeature.hasTag("man_made", "storage_tank", "chimney", "stele")
    ) {
      return false;
    }

    return null;
  }

  public static Optional<Boolean> hasMarkings(SourceFeature sourceFeature) {
    return Optional.ofNullable(orNullBooleans(
      TypeParser.parseBool((String) sourceFeature.getTag("markings")),
      TypeParser.parseBool((String) sourceFeature.getTag("lane_markings")),
      TypeParser.parseBool((String) sourceFeature.getTag("crossing:markings"))
    ));
  }
  public static void setCommonFeatureParams(FeatureCollector.Feature feature, SourceFeature sourceFeature) {
    if (sourceFeature instanceof OsmSourceFeature osmFeature) {
      OsmElement element = osmFeature.originalElement();

      feature
        .setAttr("osmId", sourceFeature.id())
        .setAttr("osmType", element instanceof OsmElement.Node ? 0 :
          element instanceof OsmElement.Way ? 1 :
            element instanceof OsmElement.Relation ? 2 : null
        );
    }

    feature
      .setZoomRange(16, 16)
      .setPixelToleranceAtAllZooms(0)
      .setMinPixelSize(0)
      .setMinPixelSizeAtMaxZoom(0)
      .setBufferPixels(4);
  }
}
