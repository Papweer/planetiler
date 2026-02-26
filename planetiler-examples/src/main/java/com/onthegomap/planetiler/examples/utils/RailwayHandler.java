package com.onthegomap.planetiler.examples.utils;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.examples.parsers.DefaultsParser;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Optional;

public class RailwayHandler {
  private static final DefaultsParser defaultsParser = new DefaultsParser();
  public static void handleRailway(SourceFeature sourceFeature, FeatureCollector features) {
    String railwayType = StreetsUtils.getFirstTagValue(sourceFeature, "railway")
      .orElseThrow();

    var feature = features.line("highways")
      .setAttr("type", "railway")
      .setAttr("railwayCategory", defaultsParser.getString("railways."+railwayType+".type")
        .orElse("railway"))
      .setAttr("railwayType", railwayType)
      .setAttr("width", getGauge(sourceFeature, railwayType));

    StreetsUtils.setCommonFeatureParams(feature, sourceFeature);
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

  private static double getGauge(SourceFeature sourceFeature, String railwayType) {
    Optional<Double> gauge = StreetsUtils.getLargestTagValue(sourceFeature, "gauge");
    if (gauge.isPresent()) {
      return gauge.get()/1000;
    }

    String gaugeValue = StreetsUtils.getFirstTagValue(sourceFeature, "gauge")
      .orElse("");
    Optional<Double> gaugeWord = defaultsParser.getDouble("railways.gauges."+gaugeValue);
    return gaugeWord
      .orElseGet(() -> defaultsParser.getDouble("railways." + railwayType + ".gauge")
        .orElse(1.435));
  }
}
