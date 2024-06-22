set -e
set -u
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/2024-06-13-beta.0/theme=places /data/theme=buildings
java -cp planetiler.jar /profiles/Buildings.java --data=/data
aws s3 cp --no-progress data/buildings.pmtiles s3://$1
# aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/2024-06-13-beta.0/theme=places /data/theme=places
# bash $SCRIPT_DIR/places.sh places.pmtiles
# aws s3 cp --no-progress places.pmtiles s3://$1