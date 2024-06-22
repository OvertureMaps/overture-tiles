import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;

public class Transportation implements OvertureProfile.Theme {

    @Override
    public void processFeature(SourceFeature source, FeatureCollector features) {
        String layer = source.getSourceLayer();
        String clazz = source.getString("class");
        String subtype = source.getString("subtype");
        if (layer.equals("connector")) {
            var point = features.point(layer);
            point.setMinZoom(13);
            OvertureProfile.addFullTags(source, point, 14);
        } else if (layer.equals("segment")) {
            int minzoom = switch (subtype) {
                case "road" -> switch (clazz) {
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
                case "water" -> 10;
                default -> 10;
            };
            var line = features.line(layer);
            line.setMinZoom(minzoom);
            line.setMinPixelSize(0);
            OvertureProfile.addFullTags(source, line, 14);
        }
    }

    @Override
    public String name() {
        return "transportation";
    }

    public static void main(String[] args) throws Exception {
        OvertureProfile.run(Arguments.fromArgsOrConfigFile(args), new Transportation());
    }
}