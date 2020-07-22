/********************************************************************************\
 * An example glue job combining processes from pregenerated glue scripts as    *
 * shown here:                                                                  *
 * https://aws.amazon.com/blogs/aws/new-serverless-streaming-etl-with-aws-glue/ *
 * and the example here:                                                        *
 * https://docs.aws.amazon.com/glue/latest/dg/glue-etl-scala-example.html       *
 * I made some changes to fit the specifics of the example, adjusted some names,*
 * added comments, and fixed spacing for readability. I also removed the        *
 * generated comments.                                                          *
 *                                                                              *
 * Author: jeremy@jeremydowens.com                                              *
\********************************************************************************/

package com.jeremydowens.AWSGlue

import com.amazonaws.services.glue.{DynamicFrame, GlueContext, DataSink, DataSource}
import com.amazonaws.services.glue.errors.CallSite
import com.amazonaws.services.glue.util.GlueArgParser
import com.amazonaws.services.glue.util.Job
import com.amazonaws.services.glue.util.JsonOptions
import java.util.Calendar
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode, SparkSession}
import org.apache.spark.sql.streaming.Trigger
import scala.collection.JavaConverters._
import org.apache.spark.sql.functions.from_json

object ExampleJob {
  def main(sysArgs: Array[String]) {


    // All of these arguments should be supplied by default but can be overridden
    // for an individual job run. The default values are set during stack creation
    val args = GlueArgParser.getResolvedOptions(sysArgs, Seq(
      "JOB_NAME",
      "BUCKET_NAME",
      "STREAM_NAME",
      "DATABASE_NAME",
      "TABLE_NAME",
      "STAGE",
      "JOB_ROLE",
      "ACCESS_KEY",
      "SECRET_KEY",
      "ACCESS_KEY",
      "SECRET_KEY"
    ).toArray)


    val spark: SparkContext = if (args("STAGE") == "test") {
      // For testing, we need to use local execution
      val conf = new SparkConf().setAppName("GlueStreamingExample").setMaster("local")
        .set("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
        .set("spark.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
        .set("spark.hadoop.fs.s3n.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
        .set("spark.hadoop.fs.AbstractFileSystem.s3.impl", "org.apache.hadoop.fs.s3a.S3A")
        .set("spark.hadoop.fs.AbstractFileSystem.s3n.impl", "org.apache.hadoop.fs.s3a.S3A")
        .set("spark.hadoop.fs.AbstractFileSystem.s3a.impl", "org.apache.hadoop.fs.s3a.S3A")
        .set("spark.hadoop.fs.s3a.access.key", args("ACCESS_KEY"))
        .set("spark.hadoop.fs.s3a.secret.key", args("SECRET_KEY"))
      new SparkContext(conf)
    } else {
      new SparkContext()
    }
    val glueContext: GlueContext = new GlueContext(spark)
    val sparkSession: SparkSession = glueContext.getSparkSession
    // Explicitly set the log level ("WARN", "INFO", "FATAL")
    spark.setLogLevel("INFO")

    // When running locally, we should not use the internal glue functions when running
    // in a local test
    if (args("STAGE") != "test") {
      Job.init(args("JOB_NAME"), glueContext, args.asJava)
    }


    // Add the Kinesis stream as a data source, using the argument as the name
    val kinesisSource: DataFrame = if (args("STAGE") != "test") sparkSession.readStream
      .format("kinesis")
      .option("streamName", args("STREAM_NAME"))
      .option("endpointUrl", "https://kinesis.us-east-1.amazonaws.com")
      .option("startingPosition", "TRIM_HORIZON")
      .load
    else {
      sparkSession.readStream
      // For local testing, this requires a connection from: https://github.com/qubole/kinesis-sql
      // It is included in the build.sbt file
        .format("kinesis")
        .option("streamName", args("STREAM_NAME"))
        .option("endpointUrl", "https://kinesis.us-east-1.amazonaws.com")
        .option("startingPosition", "TRIM_HORIZON")
        .option("awsAccessKeyId", args("ACCESS_KEY"))
        .option("awsSecretKey", args("SECRET_KEY"))
        .option("aws_iam_role", args("JOB_ROLE"))
        .load
    }

    import sparkSession.implicits._

    // We need to extract the data from the jsonpath "data", since kinesis stream
    // sources are nested objects including additional metadata. This is done using the schema that
    // is defined in our glue table. See cloudformation/primary-stack.yml for the details.
    val sourceData: DataFrame = kinesisSource.select(
      from_json(
        $"data".cast("string"),
        // The database and table name should be supplied by default
        glueContext.getCatalogSchemaAsSparkSchema(args("DATABASE_NAME"), args("TABLE_NAME"))
      ) as "data"
    ).select("data.*")

    // Add a static data source, this is just a one line csv file containing details
    // about the client_id referenced in the stream object.

    val staticData: DataFrame = sparkSession.read          // read() returns type DataFrameReader
      .format("csv")
      .option("header", "true")
      .load(s"s3://${args("BUCKET_NAME")}/data/static.csv")  // load() returns a DataFrame

    staticData.printSchema()

    // Process groups of records as they come through the stream
    glueContext.forEachBatch(sourceData, (dataFrame: Dataset[Row], batchId: Long) => {
      // Join the static data to our source data
      val joined = dataFrame.join(staticData, "client_id")

      // Get date info for partitioning
      val year: Int = Calendar.getInstance().get(Calendar.YEAR)
      val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
      val day: Int = Calendar.getInstance().get(Calendar.DATE)
      val hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      val minute: Int = Calendar.getInstance().get(Calendar.MINUTE)

      // Check for records - attempting to write empty frames throws exceptions
      if (dataFrame.count() > 0) {
        val dynamicFrame = DynamicFrame(joined, glueContext)
        // Select the fields we want. I renamed the fname and lname
        val mappedData = dynamicFrame.applyMapping(
          mappings = Seq(
            ("temperature", "double", "temperature", "double"),
            ("humidity", "double", "humidity", "double"),
            ("pressure", "double", "pressure", "double"),
            ("pitch", "double", "pitch", "double"),
            ("roll", "double", "roll", "double"),
            ("yaw", "double", "yaw", "double"),
            ("timestamp", "timestamp", "timestamp", "timestamp"),
            ("count", "long", "count", "long"),
            ("client_id", "string", "client_id", "string"),
            ("owner_fname", "string", "first_name", "string"),
            ("owner_lname", "string", "last_name", "string")
          ),
          caseSensitive = false,
          transformationContext = "applyMapping"
        )
        // Build our s3 path using the partition info
        // Glue autogeneration does this with string concat via '+'.
        // I changed to string interpolation
        val path = s"s3a://${args("BUCKET_NAME")}/output/year=${"%04d".format(year)}/month=${"%02d".format(month)}/day=${"%02d".format(day)}/hour=${"%02d".format(hour)}/"

        // Create a sink to pipe the joined object into
        val sink = glueContext.getSinkWithFormat(
          connectionType = "s3",
          options = JsonOptions(Map(
            "path" -> path
          )),
          transformationContext = "",
          format = "parquet"
        ).writeDynamicFrame(mappedData)
      }
    },
    // These options set the record grouping window and where checkpoints are stored in s3
    // Pregenerated queries use a json string, but it's cleaner in scala to use a Map argument
    JsonOptions(
      Map(
        "windowSize" -> "100 seconds",
        "checkpointLocation" -> s"s3a://${args("BUCKET_NAME")}/output/checkpoint/"
      )
    ))

    // When running locally, we should not use the internal glue functions when running
    // in a local test
    if (args("STAGE") != "test") {
      Job.commit()
    }
  }
}
