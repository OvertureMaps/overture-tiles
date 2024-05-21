import com.fasterxml.jackson.core.JsonProcessingException;
import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.parquet.AvroParquetFeature;
import com.onthegomap.planetiler.util.Downloader;
import com.onthegomap.planetiler.util.ZoomFunction;
import com.onthegomap.planetiler.overture.Struct;
import com.onthegomap.planetiler.overture.OvertureUrls;
import java.nio.file.Path;
import java.time.Instant;
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
      .overwriteOutput(Path.of("data", "base.pmtiles"));

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
        case "transportation/segment" -> processSegment(avroFeature, features);
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
    return "Overture Transportation";
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap</a>
      <a href="https://overturemaps.org" target="_blank">&copy; Overture Foundation</a>
      """
      .replaceAll("\n", " ")
      .trim();
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

  private static String join(String sep, Struct struct) {
    List<Struct> items = struct.asList();
    if (items.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (Struct item : items) {
      if (sb.length() > 0) {
        sb.append(sep);
      }
      sb.append(item.asString());
    }
    return sb.toString();
  }

  private void processSegment(AvroParquetFeature sourceFeature, FeatureCollector features) {
    Struct struct = sourceFeature.getStruct();
    var subtype = struct.get("subtype").asString();
    var roadClass = struct.get("class").asString();
    int minzoom = switch (subtype) {
      case "road" -> switch (roadClass) {
        case "motorway" -> 4;
        case "trunk" -> 5;
        case "primary" -> 7;
        case "secondary" -> 9;
        case "tertiary" -> 11;
        case "residential" -> 12;
        case "living_street" -> 13;
        default -> 14;
      };
      case "rail" -> 8;
      case "water"-> 10;
      default -> 10;
    };
    var commonTags = getCommonTags(struct);
    commonTags.put("subtype", struct.get("subtype").asString());
    commonTags.put("connectors", ZoomFunction.minZoom(14, join(",", struct.get("connectors"))));

    if (subtype != "road") {
      features.line(sourceFeature.getSourceLayer())
        .setMinZoom(minzoom)
        .setMinPixelSize(0)
        .putAttrs(commonTags);
    } else {
      commonTags.put("class", roadClass.toString());
      var feature = features.line(sourceFeature.getSourceLayer())
        .setMinZoom(minzoom)
        .setMinPixelSize(0)
        .putAttrs(commonTags)
        .setAttr("road", struct.get("road").asJson());
    }
  }


  private static FeatureCollector.Feature createAnyFeature(AvroParquetFeature sourceFeature,
    FeatureCollector features) {
    return sourceFeature.isPoint() ? features.point(sourceFeature.getSourceLayer()) :
      sourceFeature.canBePolygon() ? features.polygon(sourceFeature.getSourceLayer()) :
      features.line(sourceFeature.getSourceLayer());
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
    results.put("update_time", info.get("update_time"));
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
