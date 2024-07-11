set -e
set -u
set -o pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

duckdb -c "
load spatial;
set s3_region='us-west-2';

COPY ( 
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'administrative_boundary',
            'minzoom', CASE WHEN admin_level = 2 THEN 0 WHEN admin_level = 2 THEN 4 WHEN admin_level = 3 THEN 8 ELSE 10 END
        ) as tippecanoe,
        json_object(
            'id', id,
            'admin_level', admin_level,
            'is_maritime', is_maritime,
            'geopol_display', geopol_display,
            'version', version,
            'update_time', update_time,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('$1/theme=admins/type=administrative_boundary/*'))
    UNION ALL
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'locality',
            'minzoom', CASE WHEN locality_type = 'country' THEN 0 WHEN locality_type = 'region' THEN 4 WHEN locality_type = 'county' THEN 8 ELSE 10 END
        ) as tippecanoe,
        json_object(
            'id', id,
            '@name', json_extract_string(names, '$.primary'),
            'names', names,
            'subtype', subtype,
            'locality_type', locality_type,
            'wikidata', wikidata,
            'context_id', context_id,
            'population', population,
            'version', version,
            'update_time', update_time,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('$1/theme=admins/type=locality/*'))
    UNION ALL
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'locality_area'
        ) as tippecanoe,
        json_object(
            'id', id,
            'locality_id', locality_id,
            'version', version,
            'update_time', update_time,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('$1/theme=admins/type=locality_area/*'))
    ) TO STDOUT (FORMAT json);
" | tippecanoe -o $2 -J $SCRIPT_DIR/admins.filter.json --force --drop-densest-as-needed -z 12 --progress-interval=10
