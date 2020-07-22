/******************************************************************************\
 * A  class that runs a local execution of an AWS Glue job within a scalatest *
 * Instead of running our local executions, it is preferred to call them from *
 * a test framework, where we are able to add assertions for verification.    *
 * Taken from: https://github.com/Gamesight/aws-glue-local-scala/             *
 *                                                                            *
 * Author: jeremy@jeremydowens.com                                            *
\******************************************************************************/

import org.scalatest._
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider

class ExampleSpec extends FunSpec {
  describe("Example") {
    it("should run the job") {

      val creds = ProfileCredentialsProvider.create().resolveCredentials()


      println(s"Starting ExampleJob at ${new java.util.Date()}")

      // Trigger the execution by directly calling the main class and supplying
      // arguments. AWS Glue job arguments always begin with "--" so that the
      // resolver can correctly convert it to a Map
      com.jeremydowens.AWSGlue.ExampleJob.main(Array(
        "--JOB_NAME", "job",
        "--STAGE", "test",
        // Note that in order to fill this out, you will need to have created the stack
        // ahead of time.
        "--BUCKET_NAME", "<YOUR BUCKET NAME>",
        "--STREAM_NAME", "<YOUR STREAM NAME>",
        "--DATABASE_NAME", "<YOUR DATABASE NAME>",
        "--TABLE_NAME", "<YOUR TABLE NAME>",
        "--JOB_ROLE", "<YOUR JOB ROLE>",
        "--ACCESS_KEY", creds.accessKeyId,
        "--SECRET_KEY", creds.secretAccessKey
      ))

    }
  }
}
