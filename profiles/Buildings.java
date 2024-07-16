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
            var source0Dataset = pf.getStruct("sources").get(0).get("dataset");
            polygon.setAttr("@source_0_dataset", source0Dataset);
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