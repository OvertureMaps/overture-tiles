1. Copy the Overture Parquet dataset to your local machine to `overture` dir:
  [Use these docs](https://github.com/OvertureMaps/data/blob/main/README.md#how-to-access-overture-maps-data). You don't need Microsoft Synapse or AWS Athena.
2. Install DuckDB and [felt/tippecanoe](https://github.com/felt/tippecanoe)
3. Run [places.sh places.pmtiles](scripts/2024-05-16-beta.0/places.sh) replacing `read_parquet(...)` with the path to your Overture copy. This streams GeoParquet into tippecanoe without any intermediate file.
