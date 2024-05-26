import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.Glob;
import com.onthegomap.planetiler.reader.parquet.ParquetFeature;
import org.apache.parquet.schema.MessageType;
import java.nio.file.Path;
import java.util.List;

public class Buildings implements Profile {

  @Override
  public void processFeature(SourceFeature source, FeatureCollector features) {
    String layer = source.getSourceLayer();

    var polygon = features.polygon(layer);

    if (source instanceof ParquetFeature feature) {
      MessageType schema = feature.parquetSchema();
      for (var field : schema.getFields()) {
        var name = field.getName();
        if (name.equals("bbox") || name.equals("geometry")) continue;
        if (field.isPrimitive()) {
          polygon.inheritAttrFromSource(name);
        } else {
         polygon.setAttr(name, source.getStruct(name).asJson());
        }
      }
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