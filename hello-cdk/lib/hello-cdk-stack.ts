import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import { aws_s3 as s3, aws_ec2 as ec2 } from "aws-cdk-lib";
import { aws_batch as batch, aws_ecs as ecs } from "aws-cdk-lib";

export class HelloCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const userData = ec2.UserData.forLinux();
    userData.addCommands("echo foo > foo");

    const multipartUserData = new ec2.MultipartUserData();
    multipartUserData.addPart(ec2.MultipartBody.fromUserData(userData));

    const launchTemplate = new ec2.LaunchTemplate(this, "LaunchTemplate", {
      machineImage: ec2.MachineImage.latestAmazonLinux2023(),
      userData: multipartUserData,
    });

    const ecsJob = new batch.EcsJobDefinition(this, "JobDefn", {
      container: new batch.EcsEc2ContainerDefinition(this, "containerDefn", {
        image: ecs.ContainerImage.fromRegistry(
          "protomaps/go-pmtiles:latest",
        ),
        memory: cdk.Size.mebibytes(512),
        cpu: 1,
        command: ["show","https://data.source.coop/protomaps/openstreetmap/tiles/v3.pmtiles"]
      }),
    });

    const vpc = new ec2.Vpc(this, 'TheVPC', {
      natGateways: 0
    });

    const queue = new batch.JobQueue(this, "JobQueue", {
      computeEnvironments: [
        {
          computeEnvironment: new batch.ManagedEc2EcsComputeEnvironment(
            this,
            "managedEc2CE",
            {
              vpc: vpc,
              launchTemplate: launchTemplate
            },
          ),
          order: 1,
        },
      ]
    });
  }
}
