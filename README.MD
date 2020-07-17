# AWS GLUE STREAMING ETL EXAMPLE - Scala

This implementation is based on the solution described here: https://aws.amazon.com/blogs/aws/new-serverless-streaming-etl-with-aws-glue/

### Requirements

* Right now, this is set up to be deployed with the Serverless Framework https://www.serverless.com/. I might remove that in the future and just have a "Deploy Stack" button. While developing, this is just a convenience. Who knows, it might be a feature in the future.  
* Java 8 - Later versions of Java will not work with AWS Glue
* SBT Version 1.3.10 - get it here https://www.scala-sbt.org/index.html - for now, this is just to ensure that the glue job compiles. Tests will be added later
* Scala 2.11.1 or later
* AWS CLI https://aws.amazon.com/cli/

### Setup

1. You might need to do some fiddling with serverless setup here
1. Ensure that you have AWS credentials and the a set up.
1. `sbt clean compile` - test soon
1. `serverless deploy`
1. `aws s3 sync src/main/scala/ s3://<your s3 details here>`
1. Start the glue job from the console or with the CLI


### TODO

* Backfill cloudformation yaml to include all Glue and Kinesis resources
* Set up NPM for Serverless framework to remove some work.

### Not Included

At this stage, setup for the IoT portion will not be covered. If I get this all cleaned up for the later portions of the pipeline, I'll at the very least add a markup tutorial for getting the SenseHat working.
