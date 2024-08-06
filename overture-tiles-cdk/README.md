# Overture Tiles CDK

Overture Tiles CDK creates the AWS infrastructure for generating tiles from Overture data.

## Useful commands

* `npm run build`   compile typescript to js
* `npm run watch`   watch for changes and compile
* `npx cdk deploy`  deploy this stack to your default AWS account/region
* `npx cdk diff`    compare deployed stack with current state
* `npx cdk synth`   emits the synthesized CloudFormation template

## Prerequisites
- [AWS CLI](https://docs.aws.amazon.com/cli/)
- [AWS CDK](https://aws.amazon.com/cdk/)
  - `npm install -g aws-cdk`

## Deploying
- Update configuration in `bin/overture-tiles-cdk.ts`
- `npm run cdk bootstrap`
- `npm run cdk deploy`

## Tile generation
- Open the [AWS Batch Jobs console](console.aws.amazon.com/batch/home#jobs)
- Click `Submit new job`
- Select from the available Job Definitions. Each definition is associated with a version and theme from a past Overture release.
- Select the OvertureTilesQueue as the Job queue.
- Submit the job. Once it is complete, it will be available at `s3://BUCKET_NAME/RELEASE/THEME.pmtiles`
