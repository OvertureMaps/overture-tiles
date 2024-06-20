import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import { aws_s3 as s3, aws_ec2 as ec2 } from "aws-cdk-lib";
import { aws_batch as batch, aws_ecs as ecs } from "aws-cdk-lib";
import { aws_iam as iam } from "aws-cdk-lib";

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
      "echo '{\"data-root\": \"/docker\"}' > /etc/docker/daemon.json",
      "systemctl restart docker"
    );

    const multipartUserData = new ec2.MultipartUserData();
    multipartUserData.addPart(ec2.MultipartBody.fromUserData(userData));

    const launchTemplate = new ec2.LaunchTemplate(this, "LaunchTemplate", {
      machineImage: ecs.EcsOptimizedImage.amazonLinux2023(ecs.AmiHardwareType.ARM),
      userData: multipartUserData,
    });

    const bucket = new s3.Bucket(this, 'MyBucket');

    const role = new iam.Role(this, 'S3WriteRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    role.addToPolicy(new iam.PolicyStatement({
      actions: ['s3:PutObject', 's3:PutObjectAcl'],
      resources: [`${bucket.bucketArn}/*`],
    }));

    const ecsJob = new batch.EcsJobDefinition(this, "JobDefn", {
      container: new batch.EcsEc2ContainerDefinition(this, "containerDefn", {
        image: ecs.ContainerImage.fromRegistry(
          "protomaps/overture-tiles:latest",
        ),
        memory: cdk.Size.mebibytes(512),
        cpu: 1,
        command: [bucket.bucketName],
        jobRole: role
      }),
    });

    const vpc = new ec2.Vpc(this, 'TheVPC', {
      availabilityZones: ["us-west-1a"]
    });

    const queue = new batch.JobQueue(this, "JobQueue", {
      computeEnvironments: [
        {
          computeEnvironment: new batch.ManagedEc2EcsComputeEnvironment(
            this,
            "managedEc2CE",
            {
              vpc: vpc,
              vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
              launchTemplate: launchTemplate,
              replaceComputeEnvironment: true,
              allocationStrategy: batch.AllocationStrategy.BEST_FIT,
              instanceTypes: [ec2.InstanceType.of(ec2.InstanceClass.C6GD, ec2.InstanceSize.LARGE)],
              useOptimalInstanceClasses: false
            },
          ),
          order: 1,
        },
      ]
    });
  }
}
