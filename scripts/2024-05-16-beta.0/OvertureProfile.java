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

public class OvertureProfile implements Profile {

  public interface Theme {
    void processFeature(SourceFeature source, FeatureCollector features);
    String name();
  }

  private Theme theme;

  public OvertureProfile(Theme theme) {
    this.theme = theme;
  }

  protected static void addFullTags(SourceFeature source, FeatureCollector.Feature feature) {
    if (source instanceof ParquetFeature pf) {
      MessageType schema = pf.parquetSchema();
      for (var field : schema.getFields()) {
        var name = field.getName();
        if (name.equals("bbox") || name.equals("geometry")) continue;
        if (field.isPrimitive()) {
          feature.inheritAttrFromSource(name);
          feature.setAttrWithMinSize(name, source.getTag(name), 16);
        } else {
         feature.setAttrWithMinSize(name, source.getStruct(name).asJson(), 16);
        }
      }
    }
  }

  protected static FeatureCollector.Feature createAnyFeature(SourceFeature feature,
    FeatureCollector features) {
    return feature.isPoint() ? features.point(feature.getSourceLayer()) :
      feature.canBePolygon() ? features.polygon(feature.getSourceLayer()) :
      features.line(feature.getSourceLayer());
  }

  @Override
  public void processFeature(SourceFeature source, FeatureCollector features) {
    this.theme.processFeature(source, features);
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

  @Override
  public String name() {
    return "Overture " + this.theme.name();
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

  static void run(Arguments args, Theme theme) throws Exception {
    Path base = args.inputFile("data", "overture base directory", Path.of("data", "overture"));
    var paths = Glob.of(base).resolve("theme=" + theme.name(), "*", "*.parquet").find();
    Planetiler.create(args)
      .setProfile(new OvertureProfile(theme))
      .addParquetSource("overture",
        paths,
        true, // hive-partitioning
        fields -> fields.get("id"), // hash the ID field to generate unique long IDs
        fields -> fields.get("type")) // extract "type={}" from the filename to get layer
      .overwriteOutput(Path.of("data", theme.name() + ".pmtiles"))
      .run();
  }
}