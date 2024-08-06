import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.parquet.ParquetFeature;
import org.apache.parquet.schema.MessageType;

public class Buildings implements OvertureProfile.Theme {

    @Override
    public void processFeature(SourceFeature source, FeatureCollector features) {
        String layer = source.getSourceLayer();
        var polygon = features.polygon(layer);

        if (source instanceof ParquetFeature pf) {
            var sources = pf.getStruct("sources").asList();

            // set @height_source helper
            sources.stream().filter(s -> s.get("property").asString().equals("properties/height")).findFirst().ifPresentOrElse(
                    s -> polygon.setAttr("@height_source", s.get("dataset")),
                    () -> {
                        if (pf.hasTag("height")) {
                            sources.stream().filter(s -> s.get("property").asString().isEmpty()).findFirst().ifPresent(
                                    s -> polygon.setAttr("@height_source", s.get("dataset"))
                            );
                        }
                    }
            );

            // set @geometry_source helper
            sources.stream().filter(s -> s.get("property").asString().equals("properties/geometry")).findFirst().ifPresentOrElse(
                    s -> polygon.setAttr("@geometry_source", s.get("dataset")),
                    () -> {
                        sources.stream().filter(s -> s.get("property").asString().isEmpty()).findFirst().ifPresent(
                                s -> polygon.setAttr("@geometry_source", s.get("dataset"))
                        );
                    }
            );
        }

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