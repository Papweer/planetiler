package com.onthegomap.planetiler.examples.utils;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.reader.SourceFeature;

public class BarrierHandler {

  private static final DefaultsParser defaultsParser = new DefaultsParser();

  public static void handleBarrier(SourceFeature sourceFeature, FeatureCollector features) {
    // TODO: Expand range of barrier types supported
    String barrierTag = (String) sourceFeature.getTag("barrier");

    String fenceType = (String) sourceFeature.getTag("fence_type");
    String wallType = (String) sourceFeature.getTag("wall");

    String defaultBarrierPath;
    if (defaultsParser.getString("barriers.fence." + fenceType+".type").isPresent()) {
      defaultBarrierPath = "barriers.fence." + fenceType + ".";
    } else if (defaultsParser.getString("barriers.wall." + wallType+".type").isPresent()) {
      defaultBarrierPath = "barriers.wall." + wallType + ".";
    } else {
      defaultBarrierPath = "barriers." + barrierTag + ".";
    }

    if (fenceType != null) {
      if (fenceType.equals("temporary") && sourceFeature.id() == 1467283141) {
        System.out.println(barrierTag + fenceType + wallType);
        System.out.println(defaultBarrierPath);
        System.out.println(defaultsParser.getString("barriers.fence.temporary.type"));
        System.out.println(defaultsParser.getString(defaultBarrierPath + "material").orElse("uesinpkllre"));
      }
    }

    var feature = features.line("barriers")
      .setAttr("type", "barrier")
      .setAttr("barrierType", defaultsParser.getString(defaultBarrierPath + "type")
        .orElse("fence"))
      .setAttr("material", defaultsParser.getString(defaultBarrierPath + "material")
        .orElse("wood"))
      .setAttr("height", StreetsUtils.getHeight(sourceFeature)
        .orElse(defaultsParser.getDouble(defaultBarrierPath + "height")
          .orElse(1.0)))
      .setAttr("minHeight", StreetsUtils.getMinHeight(sourceFeature));

    StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
  }

  public static boolean isBarrier(SourceFeature sourceFeature) {
    return sourceFeature.hasTag("barrier", "fence", "hedge", "wall");
  }
}
