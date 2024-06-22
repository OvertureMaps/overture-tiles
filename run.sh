set -e
set -u
set -x

RELEASE="2024-06-13-beta.0"

#aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/$RELEASE/theme=buildings /data/theme=buildings
#java -cp planetiler.jar /profiles/Buildings.java --data=/data
#aws s3 cp --no-progress data/buildings.pmtiles s3://$1

aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/$RELEASE/theme=places /data/theme=places
bash scripts/$RELEASE/places.sh places.pmtiles
aws s3 cp --no-progress places.pmtiles s3://$1