duckdb -c "
load spatial;
load httpfs;
set s3_region='us-west-2';

COPY ( 
    SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object() AS properties 
    FROM read_parquet('s3://overturemaps-us-west-2/release/2024-04-16-beta.0/theme=places/type=place/*') 
) TO STDOUT (FORMAT json);
" | tippecanoe -o $1 --force -j '{ "*": [ "attribute-filter", "name", [ ">=", "$zoom", 9 ] ] }' -l places -rg --drop-densest-as-needed

