duckdb -c "
load spatial;
load httpfs;
set s3_region='us-west-2';

COPY ( 
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'boundary'
        ) as tippecanoe,
        json_object(
            'id', id
        ) AS properties,
        (row_number() over ())*3 as id
    FROM read_parquet('s3://overturemaps-us-west-2/release/2024-05-16-beta.0/theme=divisions/type=boundary/*') limit 100)
    UNION
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'division'
        ) as tippecanoe,
        json_object(
            'id', id
        ) AS properties,
        1 + (row_number() over ()) * 3 as id
    FROM read_parquet('s3://overturemaps-us-west-2/release/2024-05-16-beta.0/theme=divisions/type=division/*') limit 100)
    UNION 
    (SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'layer', 'division_area'
        ) as tippecanoe,
        json_object(
            'id', id
        ) AS properties,
        2+ (row_number() over ()) * 3 as id
    FROM read_parquet('s3://overturemaps-us-west-2/release/2024-05-16-beta.0/theme=divisions/type=division_area/*') limit 100)
    ) TO STDOUT (FORMAT json);
" | tippecanoe -o $1 --force -l divisions

