SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

set -e
set -u
set -o pipefail
duckdb -c "
load spatial;

COPY ( 
    SELECT
        'Feature' AS type,
        json(st_asgeojson(st_geomfromwkb(geometry))) AS geometry,
        json_object(
            'id', id,
            'street', street,
            'number', number,
            'unit', unit,
            'postcode', postcode,
            'address_levels', address_levels,
            'country', country,
            'version', version,
            'sources', sources
        ) AS properties,
        row_number() over () as id,
    FROM read_parquet('$1/theme=addresses/type=address/*')
) TO STDOUT (FORMAT json);
" | tippecanoe -o $2 --force -J $SCRIPT_DIR/addresses.filter.json -l address -rg --drop-densest-as-needed  --maximum-tile-bytes=1000000 --extend-zooms-if-still-dropping --progress-interval=10
