package com.onthegomap.planetiler.examples.utils;

import static java.lang.Math.max;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.examples.parsers.TypeParser;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Optional;

public class PathHandler {
  private static final DefaultsParser defaultsParser = new DefaultsParser();

  public static class RoadwayLanes {
    Integer lanes;
    Integer lanesForward;
    Integer lanesBackward;
    Boolean oneway;
  }

  public static void handlePath(SourceFeature sourceFeature, FeatureCollector features) {
    String pathType = StreetsUtils.getOptionalTag(sourceFeature, "highway")
      .orElseGet(() -> StreetsUtils.getOptionalTag(sourceFeature, "aeroway")
        .orElseThrow());

    String pathCategory = defaultsParser.getString("paths."+pathType+".type").orElse("roadway");

    var feature = features.line("highways")
      .setAttr("type", "path")
      .setAttr("pathCategory", pathCategory)

      .setAttr("material", getPathMaterial(sourceFeature, pathType))

      .setAttr("markings", StreetsUtils.hasMarkings(sourceFeature)
        .orElse(defaultsParser.getBoolean("paths."+pathType+".markings")
        .orElse(false)));

    if (pathCategory.equals("roadway")) {
      RoadwayLanes roadwayLanes = getRoadwayLanes(sourceFeature, pathType);
      Double width = getRoadwayWidth(sourceFeature, pathType, roadwayLanes);

      feature.setAttr("lanes", roadwayLanes.lanes)
        .setAttr("lanesForward", roadwayLanes.lanesForward)
        .setAttr("lanesBackward", roadwayLanes.lanesBackward)
        .setAttr("oneway", roadwayLanes.oneway)

        .setAttr("sidewalkSide", getRoadwayExtensionSide(sourceFeature, "sidewalk"))
        .setAttr("cyclewaySide", getRoadwayExtensionSide(sourceFeature, "cycleway"))

        .setAttr("width", width);

    } else if (pathCategory.equals("footway")) {
      feature.setAttr("width", StreetsUtils.getWidth(sourceFeature)
        .orElse(defaultsParser.getDouble("paths."+pathType+".width")
          .orElse(2.0)));
    } else {
      feature.setAttr("width", StreetsUtils.getWidth(sourceFeature)
        .orElse(defaultsParser.getDouble("paths."+pathType+".width")
        .orElse(3.0)));
    }

    StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
    feature.setBufferPixels(24);
  }

  private static String getPathMaterial(SourceFeature sourceFeature, String pathType) {
    Optional<String> surface = StreetsUtils.getSurface(sourceFeature);
    if (surface.isEmpty())  {
      surface = defaultsParser.getString("paths."+pathType+".surface");
    }

    return defaultsParser.getString("materials."+surface.orElse("asphalt")).orElse("asphalt");
  }

  private static RoadwayLanes getRoadwayLanes(SourceFeature sourceFeature, String pathType) {
    RoadwayLanes roadwayLanes = new RoadwayLanes();
    roadwayLanes.lanesForward = TypeParser.parseUnsignedInt((String) sourceFeature.getTag("lanes:forward"));
    roadwayLanes.lanesBackward = TypeParser.parseUnsignedInt((String) sourceFeature.getTag("lanes:backward"));
    roadwayLanes.oneway = StreetsUtils.getOptionalBoolTag(sourceFeature, "oneway").orElse(false)
      || sourceFeature.hasTag("junction", "roundabout");

    Optional<Integer> lanesTag = StreetsUtils.getOptionalUnsignedIntTag(sourceFeature, "lanes");
    roadwayLanes.lanes = lanesTag.orElse(
      (roadwayLanes.oneway) ? defaultsParser.getInteger("paths." + pathType + ".lanes").orElse(2) / 2
        : defaultsParser.getInteger("paths." + pathType + ".lanes").orElse(2)
    );

    if (roadwayLanes.oneway) {
      roadwayLanes.lanesForward = roadwayLanes.lanes;
      roadwayLanes.lanesBackward = 0;
    } else {
      if (roadwayLanes.lanesForward == null && roadwayLanes.lanesBackward == null) {
        roadwayLanes.lanesBackward = roadwayLanes.lanes / 2;
        roadwayLanes.lanesForward = roadwayLanes.lanes - roadwayLanes.lanesBackward;
      } else if (roadwayLanes.lanesForward == null) {
        roadwayLanes.lanesForward = max(0, roadwayLanes.lanes - roadwayLanes.lanesBackward);
      } else if (roadwayLanes.lanesBackward == null) {
        roadwayLanes.lanesBackward = max(0, roadwayLanes.lanes - roadwayLanes.lanesForward);
      }
    }

    return roadwayLanes;
  }

  private static Double getRoadwayWidth(SourceFeature sourceFeature, String pathType, RoadwayLanes lanes) {
    Double tagWidth = TypeParser.parseUnits((String) sourceFeature.getTag("width"), 1.0);
    if (tagWidth != null) {
      return tagWidth;
    }
    Optional<Double> defaultWidth = defaultsParser.getDouble("paths."+pathType+".width");
    if (defaultWidth.isPresent()) {
      return defaultWidth.get();
    }

    if (lanes.lanes == 1) {
      return 4.0;
    } else {
      return lanes.lanes * 3.0;
    }

  }

  private static String getRoadwayExtensionSide(SourceFeature sourceFeature, String extensionFeatureType) {
    String value = sourceFeature.getTag(extensionFeatureType) + "";
    String bothValue = sourceFeature.getTag(extensionFeatureType+":both") + "";
    String leftValue = sourceFeature.getTag(extensionFeatureType+":left") + "";
    String rightValue = sourceFeature.getTag(extensionFeatureType+":right") + "";

    if (bothValue.equalsIgnoreCase("yes") || value.equalsIgnoreCase("both")) {
      return "both";
    } else if (leftValue.equalsIgnoreCase("yes") || value.equalsIgnoreCase("left")) {
      return "left";
    } else if (rightValue.equalsIgnoreCase("yes") || value.equalsIgnoreCase("right")) {
      return "right";
    }
    return null;
  }
}
