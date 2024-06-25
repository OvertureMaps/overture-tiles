set -e
set -u
set -x

BUCKET=$1
THEME=$2


RELEASE_DATA="2024-06-13-beta.1"

# trim the patch version: 2024-06-13-beta.1 -> 2024-06-13-beta
RELEASE_TILESET="${RELEASE_DATA%%.*}"

aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/$RELEASE/theme=$THEME /data/theme=$THEME

if [ "$THEME" == "places" ] || [ "$THEME" == "divisions" ]; then
  bash scripts/$RELEASE_TILESET/$THEME.sh $THEME.pmtiles
  aws s3 cp --no-progress $THEME.pmtiles s3://$BUCKET/$RELEASE_TILESET
else
  className="${THEME^}"
  java -cp planetiler.jar /profiles/$className.java --data=/data
  aws s3 cp --no-progress data/$THEME.pmtiles s3://$BUCKET/$RELEASE_TILESET
fi
