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
import org.apache.spark.{SparkContext}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode, SparkSession}
import org.apache.spark.sql.streaming.Trigger
import scala.collection.JavaConverters._
import org.apache.spark.sql.functions.from_json

object ExampleJob {
  def main(sysArgs: Array[String]) {
    val spark: SparkContext = new SparkContext()
    val glueContext: GlueContext = new GlueContext(spark)
    val sparkSession: SparkSession = glueContext.getSparkSession
    // This is just for convenience. There is a lot of log output.
    spark.setLogLevel("FATAL")

    val args = GlueArgParser.getResolvedOptions(sysArgs, Seq(
      "JOB_NAME",
      "BUCKET_NAME",
      "STREAM_NAME",
      "DATABASE_NAME",
      "TABLE_NAME"
    ).toArray)
    Job.init(args("JOB_NAME"), glueContext, args.asJava)

    // Add the Kinesis stream as a data source. For our example, the stream is called
    // sensehat-records
    val kinesisSource: DataFrame = sparkSession.readStream   // readstream() returns type DataStreamReader
      .format("kinesis")
      .option("streamName", args("STREAM_NAME"))
      .option("endpointUrl", "https://kinesis.us-east-1.amazonaws.com")
      .option("startingPosition", "TRIM_HORIZON")
      .load

    import sparkSession.implicits._

    // We need to extract the data from the jsonpath "data", since kinesis stream
    // sources are nested objects including additional metadata. This is done using the schema that
    // is defined in our glue table. See serverless.yml for the details.
    val sourceData: DataFrame = kinesisSource.select(
      from_json(
        $"data".cast("string"),
        glueContext.getCatalogSchemaAsSparkSchema(args("DATABASE_NAME"), args("TABLE_NAME"))
      ) as "data"
    ).select("data.*")

    // Add a static data source, this is just a one line csv file containing details
    // about the client_id referenced in the stream object.

    val staticData = sparkSession.read          // read() returns type DataFrameReader
      .format("csv")
      .option("header", "true")
      .load(s"s3://${args("BUCKET_NAME")}/data/static.csv")  // load() returns a DataFrame

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
        val path = s"s3://${args("BUCKET_NAME")}/output/ingest_year=${"%04d".format(year)}/ingest_month=${"%02d".format(month)}/ingest_day=${"%02d".format(day)}/ingest_hour=${"%02d".format(hour)}/"
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
        "checkpointLocation" -> s"s3://${args("BUCKET_NAME")}/output/checkpoint/"
      )
    ))
    Job.commit()
  }
}
