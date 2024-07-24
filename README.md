# Overture Tiles

Create tilesets from [Overture Maps](http://overturemaps.org) data.

These tilesets display an ["X-ray" visualization of Overture data](https://explore.overturemaps.org), for inspecting the breadth of Overture data and attributes. **They are not designed to be a production-ready cartographic basemap.**

Each Overture **theme** has an associated [PMTiles](https://github.com/protomaps/PMTiles) file.

View these tilesets in your browser:
* [addresses](https://pmtiles.io/?url=https%3A%2F%2Foverturemaps-tiles-us-west-2-beta.s3.amazonaws.com%2F2024-07-22%2Faddresses.pmtiles)
* [base](https://pmtiles.io/?url=https%3A%2F%2Foverturemaps-tiles-us-west-2-beta.s3.amazonaws.com%2F2024-07-22%2Fbase.pmtiles)
* [buildings](https://pmtiles.io/?url=https%3A%2F%2Foverturemaps-tiles-us-west-2-beta.s3.amazonaws.com%2F2024-07-22%2Fbuildings.pmtiles)
* [divisions](https://pmtiles.io/?url=https%3A%2F%2Foverturemaps-tiles-us-west-2-beta.s3.amazonaws.com%2F2024-07-22%2Fdivisions.pmtiles)
* [places](https://pmtiles.io/?url=https%3A%2F%2Foverturemaps-tiles-us-west-2-beta.s3.amazonaws.com%2F2024-07-22%2Fplaces.pmtiles)
* [transportation](https://pmtiles.io/?url=https%3A%2F%2Foverturemaps-tiles-us-west-2-beta.s3.amazonaws.com%2F2024-07-22%2Ftransportation.pmtiles)

* S3: `s3://overturemaps-tiles-us-west-2-beta/RELEASE/THEME.pmtiles`
* HTTP: `https://overturemaps-tiles-us-west-2-beta.s3.amazonaws.com/RELEASE/THEME.pmtiles`

* `THEME`: one of `addresses`, `base`, `buildings`, `divisions`, `places`, `transportation`
* `RELEASE`: The Overture release name, not including minor version. For example, theme `buildings`  in **data** release `2024-07-22.0` will have tiles object name `2024-07-22/buildings.pmtiles`.

## How to Use

## Accessing only the data you want

To create a new tileset for only part of the world, use the `extract` command of the [`pmtiles` CLI](https://github.com/protomaps/go-pmtiles).

To get all `buildings` tiles around Ghent, Belgium:

```
pmtiles extract https://overturemaps-tiles-us-west-2-beta.s3.amazonaws.com/2024-07-22/buildings.pmtiles ghent.pmtiles --bbox=3.660507,51.004250,3.784790,51.065996
```

## Building Tilesets

### On AWS

Included is a [AWS CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html) configuration for automating tileset creation using [AWS Batch](https://docs.aws.amazon.com/batch/latest/userguide/Batch_GetStarted.html).

### Other Environments

#### Requirements

* a [Java Runtime Environment](), version 22+, to build the `base`, `buildings` and `transportation` themes, along with `planetiler.jar` from [onthegomap/planetiler Releases](https://github.com/onthegomap/planetiler/releases).
* the [felt/tippecanoe](https://github.com/felt/tippecanoe?tab=readme-ov-file#installation) tool and the [DuckDB CLI](https://duckdb.org/docs/installation/) for other themes.
* the [aws CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) for downloading Overture data.

#### Scripts

You can build the tilesets from raw data, modifying the `profiles/` and `scripts/`.

* Copy the Overture Parquet dataset to your local machine
  [using these docs](https://github.com/OvertureMaps/data/blob/main/README.md#how-to-access-overture-maps-data). If you want to only run on a small sample of data, you can use only the first `.parquet` file instead of all in the directory.

* for the `base`, `buildings` and `transportation` themes, generate the tileset with java:

```sh
# --data indicates where your Overture data is (overture/theme=base/...)
java -cp planetiler.jar profiles/Base.java --data=overture
```

The above command outputs `base.pmtiles` in the `data` dir.

* for other themes, run the theme script in `themes/`:

```sh
scripts/2024-07-22/places.sh overture places.pmtiles
```

This reads from Overture data in `overture` and writes `places.pmtiles`.
