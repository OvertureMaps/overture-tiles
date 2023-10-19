1. Copy the Overture Parquet dataset to your local machine to `overture` dir:
  [Use these docs](https://github.com/OvertureMaps/data/blob/main/README.md#how-to-access-overture-maps-data). You don't need Microsoft Synapse or AWS Athena.
2. Install DuckDB and run this script:
  ```sql
LOAD spatial;

COPY ( SELECT
      json_extract_string(names, '$.common[0].value') as name,
      json_extract_string(categories, '$.main') as category_main,
      round(confidence,2) as confidence,
      st_geomfromwkb(geometry)
      from read_parquet('overture/theme=places/type=place/*')) TO 'places.geojsonseq' WITH (FORMAT gdal, DRIVER 'geojsonseq');
  ```
3. Feed the `geojsonseq` into [felt/tippecanoe](https://github.com/felt/tippecanoe):

```sh
tippecanoe -o overture-pois.pmtiles places.geojsonseq --force --read-parallel  -j '{ "*": [ "attribute-filter", "name", [ ">=", "$zoom", 9 ] ] }' -l pois -rg --drop-densest-as-needed
```

Bob's your uncle!
