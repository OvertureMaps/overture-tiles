import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;

public class Base implements OvertureProfile.Theme {

  @Override
  public void processFeature(SourceFeature source, FeatureCollector features) {
    String layer = source.getSourceLayer();
    String clazz = source.getString("class");
    var feature = OvertureProfile.createAnyFeature(source, features);
    if (layer.equals("infrastructure")) {
      feature.setMinZoom(13);
      OvertureProfile.addFullTags(source, feature);
    } else if (layer.equals("land")) {
      int minzoom = 7;
      if (source.isPoint()) {
        minzoom = 13;
      } else if (clazz.equals("land") || clazz.equals("glacier")) {
        minzoom = 0;
      }
      feature.setMinZoom(minzoom);
      OvertureProfile.addFullTags(source, feature);
    } else if (layer.equals("land_use")) {
      int minzoom = 9;
      if (source.isPoint()) {
        minzoom = 13;
      } else if (clazz.equals("residential")) {
        minzoom = 6;
      }
      feature.setMinZoom(minzoom);
      OvertureProfile.addFullTags(source, feature);
    } else if (layer.equals("land_cover")) {
      var cartography = source.getStruct("cartography");
      feature.setMaxZoom(cartography.get("max_zoom").asInt());
      feature.setMinZoom(cartography.get("min_zoom").asInt());
      OvertureProfile.addFullTags(source, feature);
    } else if (layer.equals("water")) {
      int minzoom = 13;
      if (source.isPoint()) {
        if (clazz.equals("ocean")) {
          minzoom = 0;
        } else {
          minzoom = 8;
        }
      } else {
        if (clazz.equals("lake") || clazz.equals("ocean") || clazz.equals("reservoir")) {
          minzoom = 4;
        } else if (clazz.equals("river")) {
          minzoom = 9;
        } else if (clazz.equals("canal")) {
          minzoom = 12;
        } else if (clazz.equals("stream")) {
          minzoom = 13;
        }
      }
      feature.setMinZoom(minzoom);
      OvertureProfile.addFullTags(source, feature);
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

