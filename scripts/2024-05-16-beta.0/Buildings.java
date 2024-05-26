import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.Glob;
import java.nio.file.Path;
import java.util.List;

public class Buildings implements Profile {

  private static final List<String> JSON_ATTRS = List.of("sources", "names");
  private static final List<String> PRIMITIVE_ATTRS = List.of("id", "version", "update_time", "subtype", "class", "level", "has_parts", "height", "num_floors", "min_height", "min_floor", "facade_color", "facade_material", "roof_material", "roof_shape", "roof_direction", "roof_orientation", "roof_color", "eave_height", "building_id");

  @Override
  public void processFeature(SourceFeature source, FeatureCollector features) {
    String layer = source.getSourceLayer();
    var polygon =features.polygon(layer);

    for (var p : PRIMITIVE_ATTRS) {
      polygon.inheritAttrFromSource(p);
    }
    for (var p : JSON_ATTRS) {
      polygon.setAttr(p, source.getStruct(p).asJson());
    }
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

  @Override
  public String name() {
    return "Overture Buildings";
  }

  @Override
  public String description() {
    return "A tileset generated from Overture data";
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap</a>
      <a href="https://docs.overturemaps.org/attribution" target="_blank">&copy; Overture Maps Foundation</a>
      """
      .replace("\n", " ")
      .trim();
  }

  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments args) throws Exception {
    Path base = args.inputFile("base", "overture base directory", Path.of("data", "overture"));
    var paths = Glob.of(base).resolve("theme=buildings", "*", "*.parquet").find();
    Planetiler.create(args)
      .setProfile(new Buildings())
      .addParquetSource("overture-buildings",
        paths,
        true, // hive-partitioning
        fields -> fields.get("id"), // hash the ID field to generate unique long IDs
        fields -> fields.get("type")) // extract "type={}" from the filename to get layer
      .overwriteOutput(Path.of("data", "buildings.pmtiles"))
      .run();
  }
}