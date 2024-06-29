import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import { aws_s3 as s3, aws_ec2 as ec2 } from "aws-cdk-lib";
import {
  aws_cloudfront as cloudfront,
  aws_cloudfront_origins as origins,
} from "aws-cdk-lib";
import { aws_batch as batch, aws_ecs as ecs } from "aws-cdk-lib";
import { aws_iam as iam } from "aws-cdk-lib";

const ID = "OvertureTiles";

export class HelloCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      "#!/bin/bash",
      "volume_name=`lsblk -x SIZE -o NAME | tail -n 1`",
      "mkfs -t ext4 /dev/$volume_name",
      "mkdir /docker",
      "mount /dev/$volume_name /docker",
      'echo \'{"data-root": "/docker"}\' > /etc/docker/daemon.json',
      "systemctl restart docker",
    );

    const multipartUserData = new ec2.MultipartUserData();
    multipartUserData.addPart(ec2.MultipartBody.fromUserData(userData));

    const launchTemplate = new ec2.LaunchTemplate(this, `${ID}LaunchTemplate`, {
      machineImage: ecs.EcsOptimizedImage.amazonLinux2023(
        ecs.AmiHardwareType.ARM,
      ),
      userData: multipartUserData,
    });

    const bucket = new s3.Bucket(this, `${ID}Bucket`, {
      blockPublicAccess: new s3.BlockPublicAccess({
        blockPublicAcls: false,
        blockPublicPolicy: false,
        ignorePublicAcls: false,
        restrictPublicBuckets: false,
      }),
      publicReadAccess: true,
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET],
          allowedOrigins: ["*"],
        },
      ],
    });

    new cloudfront.Distribution(this, `${ID}Distribution`, {
      defaultBehavior: {
        origin: new origins.S3Origin(bucket),
      },
    });

    const role = new iam.Role(this, `${ID}WriteRole`, {
      assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
    });

    role.addToPolicy(
      new iam.PolicyStatement({
        actions: ["s3:PutObject", "s3:PutObjectAcl"],
        resources: [`${bucket.bucketArn}/*`],
      }),
    );

    for (let theme of [
      "places",
      "divisions",
      "buildings",
      "transportation",
      "base",
    ]) {
      new batch.EcsJobDefinition(this, `${ID}Job_${theme}`, {
        container: new batch.EcsEc2ContainerDefinition(
          this,
          `${ID}Container_${theme}`,
          {
            image: ecs.ContainerImage.fromRegistry(
              "protomaps/overture-tiles:latest",
            ),
            memory: cdk.Size.gibibytes(60),
            cpu: 30,
            command: [bucket.bucketName, theme],
            jobRole: role,
          },
        ),
      });
    }

    const vpc = new ec2.Vpc(this, `${ID}Vpc`, {
      maxAzs: 1,
    });

    new batch.JobQueue(this, `${ID}Queue`, {
      computeEnvironments: [
        {
          computeEnvironment: new batch.ManagedEc2EcsComputeEnvironment(
            this,
            `${ID}ComputeEnvironment`,
            {
              vpc: vpc,
              spot: false,
              vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
              launchTemplate: launchTemplate,
              replaceComputeEnvironment: true,
              allocationStrategy: batch.AllocationStrategy.BEST_FIT,
              instanceTypes: [
                ec2.InstanceType.of(
                  ec2.InstanceClass.C7GD,
                  ec2.InstanceSize.XLARGE8,
                ),
              ],
              useOptimalInstanceClasses: false,
            },
          ),
          order: 1,
        },
      ],
    });
  }
}
