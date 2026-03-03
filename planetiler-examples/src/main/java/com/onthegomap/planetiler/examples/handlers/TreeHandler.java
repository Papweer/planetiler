package com.onthegomap.planetiler.examples.handlers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.examples.StreetsUtils;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.examples.parsers.TypeParser;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Optional;

public class TreeHandler {
  private static final DefaultsParser defaultsParser = new DefaultsParser();

  public static void handleTrees(SourceFeature sourceFeature, FeatureCollector features, String type) {
    String tagTreeType = StreetsUtils.getOptionalTag(sourceFeature, "genus")
      .orElse(null);
    if (defaultsParser.getString("trees." + tagTreeType + ".type").isEmpty()) {
      tagTreeType = StreetsUtils.getOptionalTag(sourceFeature, "leaf_type").orElse("broadleaved");
      if (defaultsParser.getString("trees." + tagTreeType + ".type").isEmpty()) {
        tagTreeType = "broadleaved";
      }
    }
    Double minHeight = StreetsUtils.getMinHeight(sourceFeature);
    if (minHeight == null) {
      minHeight = 0d;
    }

    if (type.equals("node")) {
      var feature = features.point("point")
        .setAttr("type", "tree")
        .setAttr("treeType", defaultsParser.getString("trees." + tagTreeType + ".type").get())
        .setAttr("height", getTreeHeight(sourceFeature, tagTreeType) - minHeight)
        .setAttr("minHeight", minHeight);

      StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
    } else if (type.equals("way")) {
      var feature = features.line("natural")
        .setAttr("type", "treeRow")
        .setAttr("treeType", defaultsParser.getString("trees." + tagTreeType + ".type").get())
        .setAttr("height", getTreeHeight(sourceFeature, tagTreeType))
        .setAttr("minHeight", StreetsUtils.getMinHeight(sourceFeature));

      StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
    } else {
      var feature = features.polygon("natural")
        .setAttr("type", "forest")
        .setAttr("treeType", defaultsParser.getString("trees." + tagTreeType + ".type").get());

      StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
    }
  }
  public static boolean isForest(SourceFeature sourceFeature) {
      return sourceFeature.hasTag("natural", "wood") ||
        sourceFeature.hasTag("landuse", "forest") ||
        sourceFeature.hasTag("landcover", "trees");
  }

  public static Double getTreeHeight(SourceFeature sourceFeature, String treeType) {
    Optional<Double> height = StreetsUtils.getHeight(sourceFeature);
    if (height.isPresent()) {
      return height.get();
    }
    // We need minHeight in case tree height is implied from tags other than "height"
    Double minHeight = StreetsUtils.getMinHeight(sourceFeature);
    if (minHeight == null) minHeight = 0d;
    Double width = TypeParser.parseMeters((String) sourceFeature.getTag("diameter_crown"));
    if (width != null) {
      return width * 2 + minHeight;
    }
    // Diameter is in millimeters if no unit of measurement is specified
    Double diameter = TypeParser.parseMillimeters((String) sourceFeature.getTag("diameter"));
    if (diameter != null) {
      return diameter * 60 + minHeight;
    }
    Double circumference = TypeParser.parseMeters((String) sourceFeature.getTag("circumference"));
    if (circumference != null) {
      return circumference / Math.PI * 60 + minHeight;
    }

    Double minTreeHeight = defaultsParser.getDouble("trees."+treeType+".minHeight")
      .orElse(8d);
    Double maxTreeHeight = defaultsParser.getDouble("trees."+treeType+".maxHeight")
      .orElse(12d);

    // Scale Math.random to the range specified in default values (rounded to 2 decimal places)
    return Double.parseDouble(String.format("%.2f", (Math.random() * (maxTreeHeight-minTreeHeight) + minTreeHeight)));
  }
}
