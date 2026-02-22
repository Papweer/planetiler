package com.onthegomap.planetiler.examples.parsers;

public class TypeParser {
  public static Double parseDouble(String value) {
    if (value == null) return null;

    try {
      return Double.parseDouble(value);
    } catch (Exception ex) {
      return null;
    }
  }

  public static Integer parseInt(String value) {
    if (value == null) return null;

    try {
      return (int) Double.parseDouble(value);
    } catch (Exception ex) {
      return null;
    }
  }

  public static Integer parseUnsignedInt(String value) {
    Integer signedInt = parseInt(value);
    if (signedInt == null) return null;
    return Math.max(0, signedInt);
  }

  public static Boolean parseBool(String value) {
    if (value == null) return null;

    if (value.toLowerCase().matches("yes|true|1")) {
      return true;
    } else if ((value.toLowerCase().matches("no|false|0"))) {
      return false;
    } else {
      return null;
    }
  }

  public static Double parseUnits(String str, double defaultUnitsFactor) {
    if (str == null) return null;

    str = str
      .replaceAll(",", ".")
      .replaceAll(" ", "")
      .replaceAll("ft", "'")
      .replaceAll("feet", "'");

    if (str.contains("cm")) {
      Double cms = parseDouble(str.replace("cm", ""));
      return cms != null ? cms * 0.01 : null;
    } else if (str.contains("m")) {
      return parseDouble(str.replace("m", ""));
    } else if (str.contains("'")) {
      String[] parts = str.split("'");

      if (parts.length == 0) return null;

      Double feet = parseDouble(parts[0]);
      Double inches = null;

      if (parts.length > 1) {
        inches = parseDouble(parts[1]);
      }

      if (feet == null) feet = 0d;
      if (inches == null) inches = 0d;

      return (feet * 12 + inches) * 0.0254;
    } else if (str.contains("\"")) {
      Double inches = parseDouble(str.replace("\"", ""));
      return inches != null ? inches * 0.0254 : null;
    }

    Double parsed = parseDouble(str);
    return parsed != null ? parsed * defaultUnitsFactor : null;
  }

  public static Double parseMeters(String str) {
    return parseUnits(str, 1d);
  }

  public static Double parseMillimeters(String str) {
    return parseUnits(str, 0.001d);
  }
}
