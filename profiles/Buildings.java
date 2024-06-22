import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;

// TODO: no null names
// TODO: add names helper field
// TODO: remove some tags at lower zooms
public class Buildings implements OvertureProfile.Theme {

    @Override
    public void processFeature(SourceFeature source, FeatureCollector features) {
        String layer = source.getSourceLayer();
        var polygon = features.polygon(layer);
        OvertureProfile.addFullTags(source, polygon, 14);
    }

    @Override
    public String name() {
        return "buildings";
    }

    public static void main(String[] args) throws Exception {
        OvertureProfile.run(Arguments.fromArgsOrConfigFile(args), new Buildings());
    }
}