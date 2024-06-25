duckdb -c "
load spatial;
load httpfs;
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
            'update_time', update_time,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('/srv/data/overture/2024-04-16-beta.0/theme=divisions/type=boundary/*'))
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
        'country', country,
        'region', region,
        'hierarchies', hierarchies,
        'parent_division_id', parent_division_id,
        'perspectives', perspectives,
        'norms', norms,
        'population', population,
        'capital_division_id', capital_division_id,
        'wikidata', wikidata,
        'names', names,
        'version', version,
        'update_time', update_time,
        'sources', sources
    ) AS properties,
    row_number() over () as id
FROM read_parquet('/srv/data/overture/2024-04-16-beta.0/theme=divisions/type=division/*'))
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
            'update_time', update_time,
            'sources', sources
        ) AS properties,
        row_number() over () as id
    FROM read_parquet('s3://overturemaps-us-west-2/release/2024-05-16-beta.0/theme=divisions/type=division_area/*'))
    ) TO STDOUT (FORMAT json);
" | tippecanoe -o $1 -J divisions.filter.json --force --drop-densest-as-needed -z 12

