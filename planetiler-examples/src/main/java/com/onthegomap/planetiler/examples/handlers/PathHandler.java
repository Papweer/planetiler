package com.onthegomap.planetiler.examples.handlers;

import static java.lang.Math.max;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.examples.parsers.TypeParser;
import com.onthegomap.planetiler.examples.StreetsUtils;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

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

    var feature = features.line("paths")
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

  public static boolean handlePathArea(SourceFeature sourceFeature, FeatureCollector features) {
    String pathType = StreetsUtils.getOptionalTag(sourceFeature, "area:highway")
      .orElseGet(() -> StreetsUtils.getOptionalTag(sourceFeature, "area:aeroway")
        .orElseGet(() -> StreetsUtils.getOptionalTag(sourceFeature, "highway")
          .orElseGet(() -> StreetsUtils.getOptionalTag(sourceFeature, "aeroway")
            .orElseThrow())));
    String pathCategory = defaultsParser.getString("paths."+pathType+".type").orElse("roadway");

    if (sourceFeature.hasTag("area:highway") || sourceFeature.hasTag("area:aeroway")) {
      var feature = features.polygon("paths")
        .setAttr("type", "pathArea")
        .setAttr("pathCategory", pathCategory)
        .setAttr("pathType", pathType)
        .setAttr("material", getPathMaterial(sourceFeature, pathType));

      StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
      return true;
    } else if (sourceFeature.hasTag("highway")) {
      var feature = features.polygon("paths")
        .setAttr("type", "pathArea")
        .setAttr("pathCategory", pathCategory)
        .setAttr("pathType", pathType)
        .setAttr("material", getPathMaterial(sourceFeature, pathType))
        .setAttr("markings", false);

      StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
      return true;
    }
    return false;
  }

  public static void postProcessAreas(List<VectorTile.Feature> items) {
    // Check if tile has path areas at all to reduce unneeded processing
    boolean hasPathAreas = false;
    for (VectorTile.Feature item : items) {
      if (item.tags().get("type").equals("pathArea")) {
        hasPathAreas = true;
        break;
      }
    }
    if (!hasPathAreas) {
      return;
    }

    List<VectorTile.Feature> pathAreas = new ArrayList<>();
    List<VectorTile.Feature> pathLines = new ArrayList<>();

    for (VectorTile.Feature item : items) {
      if (item.tags().get("type").equals("pathArea")) {
        pathAreas.add(item);
      } else {
        pathLines.add(item);
      }
    }

    // Remove all path lines from items; processed versions will be added back at the end
    items.removeIf(item -> !item.tags().get("type").equals("pathArea"));

    var insideSegments = new ArrayList<VectorTile.Feature>();

    for (VectorTile.Feature area : pathAreas) {
      Geometry decodedArea;
      try {
        decodedArea = area.geometry().decode();
      } catch (GeometryException ignored) {
        continue;
      }
      PreparedGeometry prep = PreparedGeometryFactory.prepare(decodedArea);

      // Get all lines that intersect the area and sort them by the intersection length
      List<VectorTile.Feature> intersectingLines = pathLines.stream()
        .filter(line -> {
          try {
            return prep.intersects(line.geometry().decode());
          } catch (GeometryException e) {
            return false;
          }
        })
        .sorted(Comparator.comparingDouble((VectorTile.Feature line) -> {
          try {
            return line.geometry().decode().intersection(decodedArea).getLength();
          } catch (GeometryException ignored) {
            return 0.0;
          }
        }).reversed())
        .toList();


      // Tags to transfer from line to area
      String[] tags = {"material", "width" }; // Width used for texture scaling

      for (String tag : tags) {
        if (area.getTag(tag) != null) {
          continue;
        }
        // Prioritize the values of longer segments
        for (VectorTile.Feature line : intersectingLines) {
          Object value = line.getTag(tag);
          if (value != null) {
            area.setTag(tag, value);
            break;
          }
        }
      }

      // Split lines on the intersection and add the hasArea tag only on the inside section.
      // Outside segments are kept in the working set so they can be re-split by subsequent areas.
      var intersectingSet = new HashSet<>(intersectingLines);
      var nextLines = new ArrayList<VectorTile.Feature>();
      for (VectorTile.Feature line : pathLines) {
        if (!intersectingSet.contains(line)) {
          // Line does not intersect this area, carry forward unchanged
          nextLines.add(line);
          continue;
        }

        Geometry lineGeometry;
        try {
          lineGeometry = line.geometry().decode();
        } catch (GeometryException ignored) {
          continue;
        }

        Geometry insideGeom = lineGeometry.intersection(decodedArea);
        if (!insideGeom.isEmpty()) {
          for (int n = 0; n < insideGeom.getNumGeometries(); n++) {
            VectorTile.Feature inside = line.copyWithNewGeometry(insideGeom.getGeometryN(n))
              .copyWithExtraAttrs(Map.of("hasArea", true));
            insideSegments.add(inside);
          }
        }

        Geometry outsideGeom = lineGeometry.difference(decodedArea);
        if (!outsideGeom.isEmpty()) {
          for (int n = 0; n < outsideGeom.getNumGeometries(); n++) {
            // Outside segments go back into the working set for the next area
            nextLines.add(line.copyWithNewGeometry(outsideGeom.getGeometryN(n)));
          }
        }
      }

      pathLines = nextLines;
    }

    // Add all inside segments (with hasArea) and remaining outside/non-intersecting lines back
    items.addAll(insideSegments);
    items.addAll(pathLines);
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

  public static boolean isPathArea(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("area:highway")
      || sourceFeature.hasTag("area:aeroway")
      || (sourceFeature.hasTag("highway") && StreetsUtils.getOptionalBoolTag(sourceFeature, "area").orElse(false))
      || (sourceFeature.hasTag("aeroway") && StreetsUtils.getOptionalBoolTag(sourceFeature, "area").orElse(false));
  }
}
