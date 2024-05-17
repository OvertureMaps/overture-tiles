import com.fasterxml.jackson.core.JsonProcessingException;
import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.parquet.AvroParquetFeature;
import com.onthegomap.planetiler.util.Downloader;
import com.onthegomap.planetiler.util.ZoomFunction;
import com.onthegomap.planetiler.overture.Struct;
import com.onthegomap.planetiler.overture.OvertureUrls;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Overture implements Profile {

  private static final Logger LOGGER = LoggerFactory.getLogger(Overture.class);

  private final PlanetilerConfig config;

  Overture(PlanetilerConfig config) {
    this.config = config;
  }

  public static void main(String[] args) throws Exception {
    var base = Path.of("data", "sources", "overture-2024-04-16-beta.0");
    var arguments = Arguments.fromEnvOrArgs(args).orElse(Arguments.of(Map.of(
      "tile_warning_size_mb", "10"
    )));
    var sample = arguments.getBoolean("sample", "only download smallest file from parquet source", false);
    var release = arguments.getString("release", "overture release", "2024-04-16-beta.0");

    var pt = Planetiler.create(arguments)
      .addAvroParquetSource("overture", base)
      .setProfile(planetiler -> new Overture(planetiler.config()))
      .overwriteOutput(Path.of("data", "output.pmtiles"));

    if (arguments.getBoolean("download", "download overture files", false)) {
      downloadFiles(base, pt, release, sample);
    }

    pt.run();
  }

  private static void downloadFiles(Path base, Planetiler pt, String release, boolean sample) {
    var d = Downloader.create(pt.config());
    var urls = sample ?
      OvertureUrls.sampleSmallest(pt.config(), "release/" + release) :
      OvertureUrls.getAll(pt.config(), "release/" + release);
    for (var url : urls) {
      String s = url.replaceAll("^.*" + Pattern.quote(release + "/"), "");
      var p = base.resolve(s);
      d.add(s, "https://overturemaps-us-west-2.s3.amazonaws.com/release/" + release + "/" + s, p);
    }
    var begin = pt.stats().startStage("download");
    d.run();
    begin.stop();
  }

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
    if (sourceFeature instanceof AvroParquetFeature avroFeature) {
      switch (sourceFeature.getSourceLayer()) {
        case "buildings/building", "buildings/building_part" -> processBuilding(avroFeature, features);
      }
    }
  }

  private static Map<String, Object> getSourceTags(AvroParquetFeature sourceFeature) {
    Map<String, Object> result = new HashMap<>();
    for (var entry : sourceFeature.getStruct().get("sourceTags").asMap().entrySet()) {
      result.put("sourceTags." + entry.getKey(), entry.getValue());
    }
    return result;
  }

  @Override
  public String name() {
    return "Overture Buildings";
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap</a>
      <a href="https://overturemaps.org/overture-december-2023-release-notes/" target="_blank">&copy; Overture Foundation</a>
      """
      .replaceAll("\n", " ")
      .trim();
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

  private void processBuilding(AvroParquetFeature sourceFeature, FeatureCollector features) {
    if (sourceFeature.canBePolygon()) {
      Struct struct = sourceFeature.getStruct();
      var commonTags = getCommonTags(struct);
      commonTags.put("class", struct.get("class").asString());
      commonTags.put("subtype", struct.get("subtype").asString());
      var feature = features.polygon(sourceFeature.getSourceLayer())
        .setMinZoom(11)
        .setMinPixelSize(1)
        .putAttrs(commonTags)
        .setAttr("height", struct.get("height").asDouble())
        .setAttr("min_height", struct.get("min_height").asDouble())
        .setAttr("num_floors", struct.get("num_floors").asInt())
        .setAttr("min_floor", struct.get("min_floor").asInt())
        .setAttr("roof_shape", struct.get("roof_shape").asString())
        .setAttr("roof_orientation", struct.get("roof_orientation").asString())
        .setAttr("roof_direction", struct.get("roof_direction").asDouble())
        .setAttr("eave_height", struct.get("eave_height").asDouble())
        .setAttr("roof_material", struct.get("roof_material").asString())
        .setAttr("facade_material", struct.get("facade_material").asString())
        .setAttr("facade_color", struct.get("facade_color").asString())
        .setAttr("roof_color", struct.get("roof_color").asString());
      if (Boolean.TRUE.equals(struct.get("has_parts").asBoolean())) {
        feature.setAttr("has_parts", true);
      }
      var names = getNames(struct.get("names"));
      if (!names.isEmpty()) {
        features.centroidIfConvex(sourceFeature.getSourceLayer())
          .setMinZoom(14)
          .putAttrs(names)
          .putAttrs(commonTags);
      }
    }
  }

  private static Map<String, Object> getNames(Struct names) {
    return getNames(null, names);
  }

  private static Map<String, Object> getNames(String prefix, Struct names) {
    if (names.isNull()) {
      return Map.of();
    }
    String base = prefix == null ? "name" : (prefix + ".name");
    Map<String, Object> result = new LinkedHashMap<>();
    boolean first = true;
    for (String key : List.of("common", "official", "short", "alternate")) {
      for (var name : names.get(key).asList()) {
        String value = name.get("value").asString();
        if (value != null) {
          if (first) {
            first = false;
            put(result, "name", value);
          }
          put(result, base + "." + key + "." + name.get("language").asString(), value);
        }
      }
    }
    return result;
  }

  private static void put(Map<String, Object> attrs, String key, Object value) {
    int n = 1;
    String result = key;
    while (attrs.containsKey(result)) {
      result = key + "." + (++n);
    }
    attrs.put(result, value);
  }

  private Map<String, Object> getCommonTags(Struct info) {
    Map<String, Object> results = HashMap.newHashMap(4);
    results.put("version", info.get("version").asInt());
    results.put("update_time", Instant.ofEpochMilli(info.get("update_time").asLong()).toString());
    results.put("id", ZoomFunction.minZoom(14, info.get("id").asString()));
    results.put("sources", info.get("sources").asList().stream().map(d -> {
      String recordId = d.get("recordId").asString();
      if (recordId == null) {
        recordId = d.get("recordid").asString();
      }
      return d.get("dataset").asString() + (recordId == null ? "" : (":" + recordId));
    }).sorted().distinct().collect(Collectors.joining(",")));
    results.put("source", info.get("sources").asList().stream()
      .map(d -> d.get("dataset").asString())
      .sorted()
      .distinct().collect(Collectors.joining(","))
    );
    results.put("level", info.get("level").asInt());
    return results;
  }
}
