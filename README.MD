# AWS GLUE STREAMING ETL EXAMPLE - Scala

This implementation is based on the solution described here: https://aws.amazon.com/blogs/aws/new-serverless-streaming-etl-with-aws-glue/ and includes some details from https://docs.aws.amazon.com/glue/latest/dg/glue-etl-scala-example.html.

### Requirements

* Java 8 - Later versions of Java will not work with AWS Glue
* SBT Version 1.3.10 - get it here https://www.scala-sbt.org/index.html - for now, this is just to ensure that the glue job compiles. Tests will be added later
* Scala 2.11.1 or later
* AWS CLI https://aws.amazon.com/cli/

### Setup

1. Launch the stack  
[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=GlueStreamingExample&templateURL=https://aws-glue-streaming-example.s3.amazonaws.com/cloudformation/primary-stack.yml)
1. Enter names for your resources using the cloudformation wizard
1. Upload the scripts and data to your new s3 bucket
1. Set your IoT device to publish the MQTT upload to the new Kinesis stream


### TODO

* Harden IAM policies
* Reconfigure the glue job to accept the storage bucket as an argument
* Local tests?
* Improve setup guide, add images, etc

### Not Included

At this stage, setup for the IoT portion will not be covered. If I get this all cleaned up for the later portions of the pipeline, I'll at the very least add a markup tutorial for getting the SenseHat working.

### Dev log

2020-07-18 Removed dependency on the Serverless Framework, set up parameterized cloudformation stack with 1-click launch, set up github actions to replace resources

2020-07-17 Replaced console created resources with Cloudformation, added a join to a static data source, added comments and improved Readme.  

2020-07-16 I set up the IoT connections on the Raspberry Pi and started the SenseHat collection and publishing to the cloud. Started this repository and built a prototype using the console.  

2020-07-15 Raspberry Pi with SenseHat arrived. I assembled the pieces and ensured that the hardware functioned.  
