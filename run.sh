set -e
set -u
set -x

# Automation script for running inside Docker on AWS Batch.

RELEASE_DATA=$1
SRC_BUCKET_FOLDER=$2
DEST_BUCKET_FOLDER=$3
THEME=$4

# The most recent major version used in the /scripts directory
SCRIPTS_VERSION="2024-07-22"

# Trim the patch version: 2024-06-13-beta.1 -> 2024-06-13-beta
RELEASE_TILESET="${RELEASE_DATA%%.*}"

# Download the full theme to /data.
aws s3 sync --no-progress --region us-west-2 s3://$SRC_BUCKET_FOLDER/ /data/theme=$THEME

# Tile and upload the theme to the target bucket.
if [ "$THEME" == "admins" ] || [ "$THEME" == "places" ] || [ "$THEME" == "divisions" ]; then
  bash scripts/$SCRIPTS_VERSION/$THEME.sh /data $THEME.pmtiles

  aws s3 cp --no-progress $THEME.pmtiles s3://$DEST_BUCKET_FOLDER/$THEME.pmtiles
else
  className="${THEME^}"
  java -cp planetiler.jar /profiles/$className.java --data=/data
  aws s3 cp --no-progress /data/$THEME.pmtiles s3://$DEST_BUCKET_FOLDER/$THEME.pmtiles
fi
