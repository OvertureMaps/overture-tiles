set -e
set -u
set -x

# Automation script for running inside Docker on AWS Batch.

RELEASE_DATA=$1
BUCKET=$2
THEME=$3

# The most recent major version used in the /scripts directory
SCRIPTS_VERSION="2024-07-22"

# Trim the patch version: 2024-06-13-beta.1 -> 2024-06-13-beta
RELEASE_TILESET="${RELEASE_DATA%%.*}"

# Download the full theme to /data.
aws s3 sync --no-progress --region us-west-2 --no-sign-request s3://overturemaps-us-west-2/release/$RELEASE_DATA/theme=$THEME /data/theme=$THEME

# Tile and upload the theme to the target bucket.
if [ "$THEME" == "admins" ] || [ "$THEME" == "places" ] || [ "$THEME" == "divisions" ]; then
  # Target a specific set of release scripts for generating tiles
  if test -d scripts/$RELEASE_TILESET/$THEME.sh; then
    bash scripts/$RELEASE_TILESET/$THEME.sh /data $THEME.pmtiles
  # Generate tiles using the latest release scripts
  else
    bash scripts/$SCRIPTS_VERSION/$THEME.sh /data $THEME.pmtiles
  fi

  aws s3 cp --no-progress $THEME.pmtiles s3://$BUCKET/$RELEASE_TILESET/$THEME.pmtiles
else
  className="${THEME^}"
  java -cp planetiler.jar /profiles/$className.java --data=/data
  aws s3 cp --no-progress /data/$THEME.pmtiles s3://$BUCKET/$RELEASE_TILESET/$THEME.pmtiles
fi
