import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;

public class Base implements OvertureProfile.Theme {

  @Override
  public void processFeature(SourceFeature source, FeatureCollector features) {
    String layer = source.getSourceLayer();
    if (layer.equals("infrastructure")) {
      var feature = OvertureProfile.createAnyFeature(source, features);
      feature.setMinZoom(9);
      OvertureProfile.addFullTags(source, feature);
    } else if (layer.equals("land")) {
    } else if (layer.equals("land_use")) {

    } else if (layer.equals("land_cover")) {
      var feature = OvertureProfile.createAnyFeature(source, features);
      OvertureProfile.addFullTags(source, feature);
    } else if (layer.equals("water")) {

    }
  }

  @Override
  public String name() {
    return "base";
  }

  public static void main(String[] args) throws Exception {
    OvertureProfile.run(Arguments.fromArgsOrConfigFile(args), new Base());
  }
}

