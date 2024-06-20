set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/2024-06-13-beta.0/theme=places /data/theme=places
echo $SCRIPT_DIR $1
sh $SCRIPT_DIR/places.sh places.pmtiles
aws s3 cp places.pmtiles s3://$1