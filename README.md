# Overture Tiles

Create tilesets from [Overture Maps](http://overturemaps.org) data.

These tilesets display an "X-ray" visualization of Overture data, for inspecting the breadth of Overture data and attributes. **They are not designed to be a production-ready cartographic basemap.**

Each Overture **theme** has an associated [PMTiles](https://github.com/protomaps/PMTiles) file.

View these tilesets in your browser:
* [base](https://pmtiles.io/?url=https%3A%2F%2Fhellocdkstack-overturetilesbucket6f38c611-6zusoghoh4au.s3.us-west-2.amazonaws.com%2F2024-06-13-beta%2Fbase.pmtiles)
* [buildings](https://pmtiles.io/?url=https%3A%2F%2Fhellocdkstack-overturetilesbucket6f38c611-6zusoghoh4au.s3.us-west-2.amazonaws.com%2F2024-06-13-beta%2Fbuildings.pmtiles)
* [divisions](https://pmtiles.io/?url=https%3A%2F%2Fhellocdkstack-overturetilesbucket6f38c611-6zusoghoh4au.s3.us-west-2.amazonaws.com%2F2024-06-13-beta%2Fdivisions.pmtiles)
* [places](https://pmtiles.io/?url=https%3A%2F%2Fhellocdkstack-overturetilesbucket6f38c611-6zusoghoh4au.s3.us-west-2.amazonaws.com%2F2024-06-13-beta%2Fplaces.pmtiles)
* [transportation](https://pmtiles.io/?url=https%3A%2F%2Fhellocdkstack-overturetilesbucket6f38c611-6zusoghoh4au.s3.us-west-2.amazonaws.com%2F2024-06-13-beta%2Ftransportation.pmtiles)

* S3: `s3://example-bucket/RELEASE/THEME.pmtiles`
* HTTP: `https://example-bucket.s3.amazonaws.com/RELEASE/THEME.pmtiles`

* `THEME`: one of `base`, `buildings`, `divisions`, `places`, `transportation`
* `RELEASE`: The Overture release name, not including minor version. For example, theme `buildings`  in **data** release `2024-06-13-beta.0` will have tiles object name `2024-06-13-beta/buildings.pmtiles`.

## How to Use

## Accessing only the data you want

To create a new tileset for only part of the world, use the `extract` command of the [`pmtiles` CLI](https://github.com/protomaps/go-pmtiles).

To get all `buildings` tiles around Ghent, Belgium:

`pmtiles extract https://.../buildings.pmtiles`

## Building Tilesets

### On AWS

Included is a [AWS CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html) configuration for automating tileset creation using [AWS Batch](https://docs.aws.amazon.com/batch/latest/userguide/Batch_GetStarted.html).

### Other Environments

#### Requirements

* a [Java Runtime Environment](), version 22+, to build the `base`, `buildings` and `transportation` themes.
* the [felt/tippecanoe](https://github.com/felt/tippecanoe?tab=readme-ov-file#installation) tool and the [DuckDB CLI](https://duckdb.org/docs/installation/) for other themes.
* the [aws CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) for downloading Overture data.

#### Scripts

You can build the tilesets from raw data, modifying the `profiles/` and `scripts/`.

1. Copy the Overture Parquet dataset to your local machine to `overture` dir:
  [Use these docs](https://github.com/OvertureMaps/data/blob/main/README.md#how-to-access-overture-maps-data). You don't need Microsoft Synapse or AWS Athena.
2. Install DuckDB and [felt/tippecanoe](https://github.com/felt/tippecanoe)
3. Run [places.sh places.pmtiles](scripts/2024-05-16-beta.0/places.sh) replacing `read_parquet(...)` with the path to your Overture copy. This streams GeoParquet into tippecanoe without any intermediate file.

