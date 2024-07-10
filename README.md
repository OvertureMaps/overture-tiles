# Overture Tiles

Create tilesets from [Overture Maps](http://overturemaps.org) data.

These tilesets display an "X-ray" visualization of Overture data, intended to let viewers inspect the breadth of Overture data and attributes. They are not designed to be a production-ready cartographic basemap.

Each Overture **theme** has an associated [PMTiles](https://github.com/protomaps/PMTiles) file:

The URL for `https://example-bucket.us-west-2.s3.amazonaws.com/RELEASE/THEME.pmtiles`: (not live yet)

* `THEME`: one of `base`, `buildings`, `divisions`, `places`, `transportation`
* `RELEASE`: The Overture release name, not including minor version. For example, theme `buildings`  in **data** release `2024-06-13-beta.0` will have tiles object name `2024-06-13-beta/buildings.pmtiles`.

## How to Use

To extract only a part of the tileset, use the [`pmtiles` CLI.](https://github.com/protomaps/go-pmtiles)

## Building Tilesets

### On AWS

Included is a [AWS CDK]() configuration for automating tileset creation using [AWS Batch]().

### Other Environments

#### Requirements

* a [Java Runtime Environment](), version 22+, to build the `base`, `buildings` and `transportation` themes.
* the [felt/tippecanoe](https://github.com/felt/tippecanoe) tool and the [DuckDB CLI]() for other themes. See installation
* the [aws]() command-line tool for downloading Overture data.

#### Scripts

You can build the tilesets from raw data, modifying the `profiles/` and `scripts/`.

1. Copy the Overture Parquet dataset to your local machine to `overture` dir:
  [Use these docs](https://github.com/OvertureMaps/data/blob/main/README.md#how-to-access-overture-maps-data). You don't need Microsoft Synapse or AWS Athena.
2. Install DuckDB and [felt/tippecanoe](https://github.com/felt/tippecanoe)
3. Run [places.sh places.pmtiles](scripts/2024-05-16-beta.0/places.sh) replacing `read_parquet(...)` with the path to your Overture copy. This streams GeoParquet into tippecanoe without any intermediate file.

