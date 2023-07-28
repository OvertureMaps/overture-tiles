1. Copy the Overture Parquet dataset to your local machine to `overture` dir:
  [Use these docs](https://github.com/OvertureMaps/data/blob/main/README.md#how-to-access-overture-maps-data). You don't need Microsoft Synapse or AWS Athena.
2. Install DuckDB and run this script:
  ```sql
COPY (select json_extract(bbox,'$.minx') as x,
        json_extract(bbox,'$.miny') as y,
        json_extract_string(names, '$.common[0].value') as name,
        json_extract_string(categories, '$.main') as category_main,
        from read_parquet('overture/theme=places/type=place/*')) TO 'pois.csv' (HEADER, DELIMITER ',');
  ```
3. Feed `pois.csv` into [felt/tippecanoe](https://github.com/felt/tippecanoe):

```sh
tippecanoe -o overture-pois.pmtiles pois.csv -M 80000 --drop-densest-as-needed
```

Bob's your uncle!