package com.onthegomap.planetiler.examples.handlers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.examples.StreetsUtils;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Arrays;

public class WaterHandler {
  private static final DefaultsParser defaultsParser = new DefaultsParser();

  public static void handleWaterway(SourceFeature sourceFeature, FeatureCollector features) {
    String waterwayType = StreetsUtils.getFirstTagValue(sourceFeature, "waterway")
      .orElse("stream");

    var feature = features.line("water")
      .setAttr("type", "waterway")
      .setAttr("waterwayType", waterwayType)
      .setAttr("width", StreetsUtils.getWidth(sourceFeature)
        .orElse(defaultsParser.getDouble("waterways."+waterwayType+".width")
          .orElse(2.0)));

      StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
  }

  public static boolean handleWaterArea(SourceFeature sourceFeature, FeatureCollector features) {
    var feature = features.polygon("water")
      .setAttr("type", "water");

    setWaterFeatureParams(feature, sourceFeature);
    return true;
  }

  public static boolean isWater(SourceFeature sourceFeature) {
    return sourceFeature.getSource().equals("water") ||
      sourceFeature.hasTag("natural", "water") ||
      (
        sourceFeature.hasTag("leisure", "swimming_pool") &&
          !sourceFeature.hasTag("location", "indoor", "roof")
      );
  }

  private static void setWaterFeatureParams(FeatureCollector.Feature feature, SourceFeature sourceFeature) {
    feature
      .setZoomRange(9, 16)
      .setZoomLevels(Arrays.asList(9, 13, 16))
      .setPixelToleranceAtAllZooms(0)
      .setMinPixelSize(0)
      .setMinPixelSizeAtMaxZoom(0)
      .setMinPixelSizeBelowZoom(13, 2);
  }
}
