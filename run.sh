set -e
set -u
set -x

RELEASE="2024-06-13-beta.0"
BUCKET=$1
THEME=$2

aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/$RELEASE/theme=$THEME /data/theme=$THEME

if [ "$THEME" == "places" ] || [ "$THEME" == "divisions" ]; then
  bash scripts/$RELEASE/$THEME.sh $THEME.pmtiles
else
  className="${THEME^}"
  java -cp planetiler.jar /profiles/$className.java --data=/data
fi

aws s3 cp --no-progress $THEME.pmtiles s3://$BUCKET