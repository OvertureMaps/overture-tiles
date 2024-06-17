duckdb -c "
load spatial;

COPY ( 
    SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'id', id,
            '@name', json_extract_string(names, '$.primary'),
            '@category', json_extract_string(categories, '$.main'),
            'names', names,
            'confidence', confidence,
            'categories', categories,
            'websites', websites,
            'socials', socials,
            'emails', emails,
            'phones', phones,
            'brand', brand,
            'addresses', addresses,
            'version', version,
            'update_time', update_time,
            'sources', sources
        ) AS properties,
        row_number() over () as id,
    FROM read_parquet('/srv/data/overture/2024-06-13-beta.0/theme=places/type=place/*')
) TO STDOUT (FORMAT json);
" | tippecanoe -o $1 --force -J places.filter.json -l place -rg --drop-densest-as-needed --extend-zooms-if-still-dropping

