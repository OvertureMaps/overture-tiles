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
            'layer', 'boundary',
            'minzoom', CASE WHEN subtype = 'country' THEN 0 WHEN subtype = 'region' THEN 4 WHEN subtype = 'county' THEN 8 ELSE 10 END
        ) as tippecanoe,
        json_object(
            'id', id,
            'subtype', subtype,
            'class', class,
            'divisions', divisions,
            'version', version,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('$1/theme=divisions/type=boundary/*'))
    UNION ALL
    (SELECT
    'Feature' AS type,
    json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
    json_object(
        'layer', 'division',
        'minzoom', CASE WHEN subtype = 'country' THEN 0 WHEN subtype = 'region' THEN 4 WHEN subtype = 'county' THEN 8 ELSE 10 END
    ) as tippecanoe,
    json_object(
        'id', id,
        '@name', json_extract_string(names, '$.primary'),
        'subtype', subtype,
        'local_type', local_type,
        'country', country,
        'region', region,
        'hierarchies', hierarchies,
        'parent_division_id', parent_division_id,
        'perspectives', perspectives,
        'norms', norms,
        'population', population,
        'capital_division_ids', capital_division_ids,
        'wikidata', wikidata,
        'names', names,
        'version', version,
        'sources', sources
    ) AS properties,
    row_number() over () as id
FROM read_parquet('$1/theme=divisions/type=division/*'))
    UNION ALL
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'division_area',
            'minzoom', CASE WHEN subtype = 'country' THEN 0 WHEN subtype = 'region' THEN 4 WHEN subtype = 'county' THEN 8 ELSE 10 END
        ) as tippecanoe,
        json_object(
            'id', id,
            'subtype', subtype,
            'class', class,
            'division_id', division_id,
            'country', country,
            'region', region,
            'names', names,
            'version', version,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('$1/theme=divisions/type=division_area/*'))
    ) TO STDOUT (FORMAT json);
" | tippecanoe -o $2 -J $SCRIPT_DIR/divisions.filter.json --force --drop-densest-as-needed -z 12 --progress-interval=10

