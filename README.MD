# AWS GLUE STREAMING ETL EXAMPLE - Scala

This implementation is based on the solution described here: https://aws.amazon.com/blogs/aws/new-serverless-streaming-etl-with-aws-glue/ and includes some details from https://docs.aws.amazon.com/glue/latest/dg/glue-etl-scala-example.html.

## WARNING

Please don't launch this app and leave an AWS Glue job running 24/7 just to consume a few bytes of JSON from a single IoT device. Get this working, and turn it off. Glue does a lot of great things, but it gets expensive quickly. This is not a real use case for this tech. It is just an example to help people get a head start on deployment.

### Requirements

* Java 8 - Later versions of Java will not work with AWS Glue
* SBT Version 1.3.10 - get it here https://www.scala-sbt.org/index.html - for now, this is just to ensure that the glue job compiles. Tests will be added later
* Scala 2.11.1 or later
* AWS CLI https://aws.amazon.com/cli/

### Setup

1. Launch the stack  
[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=GlueStreamingExample&templateURL=https://aws-glue-streaming-example.s3.amazonaws.com/cloudformation/primary-stack.yml)
1. Enter names for your resources using the cloudformation wizard
1. Upload the scripts and data to your new s3 bucket `aws s3 sync s3://aws-glue-streaming-example/ s3://<YOUR-BUCKET-NAME>/`
1. Set your IoT device to publish the MQTT upload to the new Kinesis stream
1. Start your glue job
1. Turn everything off!

### Running Locally

You can run the script locally by configuring /src/test/scala/ExampleSpec.scala with your details. For a small project like this, you could actually just use local execution (or deployed to EC2?) to complete the workload without paying the Glue costs which only become reasonable when you hit a certain scale. Once you have built your stack and configured the test file, use `sbt test` to run the job. For now, you will need to cancel the job manually or it will run forever (or until it crashes for an unknown reason).

### TODO

* Comments for clarity surrounding extra code that only runs when executing Locally
* Set up mocks/temporary streams and allow for real testing
* Improve setup guide, add images, etc

### Not Included

At this stage, setup for the IoT portion will not be covered. If I get this all cleaned up for the later portions of the pipeline, I'll at the very least add a markup tutorial for getting the SenseHat working.

### Dev log

2020-07-22 Added local run support, fixed job to work both locally and in glue, some doc updates to go with the new stuff  

2020-07-19 Updated the IAM policy for the glue job to restrict much of the access. In initial testing, I left it open, but now it should be much more specific. There might be some additional things to add here - feedback is always welcome.

2020-07-18 Removed dependency on the Serverless Framework, set up parameterized cloudformation stack with 1-click launch, set up github actions to replace resources, rebuilt the glue job to use resources as defined by the cloudformation stack parameters

2020-07-17 Replaced console created resources with Cloudformation, added a join to a static data source, added comments and improved Readme.  

2020-07-16 I set up the IoT connections on the Raspberry Pi and started the SenseHat collection and publishing to the cloud. Started this repository and built a prototype using the console.  

2020-07-15 Raspberry Pi with SenseHat arrived. I assembled the pieces and ensured that the hardware functioned.  
