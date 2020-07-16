package com.jeremydowens.AWSGlue

import com.amazonaws.services.glue.DynamicFrame
import com.amazonaws.services.glue.GlueContext
import com.amazonaws.services.glue.errors.CallSite
import com.amazonaws.services.glue.util.GlueArgParser
import com.amazonaws.services.glue.util.Job
import com.amazonaws.services.glue.util.JsonOptions
import java.util.Calendar
import org.apache.spark.SparkContext
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.streaming.Trigger
import scala.collection.JavaConverters._

object GlueApp {
  def main(sysArgs: Array[String]) {
    val spark: SparkContext = new SparkContext()
    val glueContext: GlueContext = new GlueContext(spark)
    // @params: [JOB_NAME]
    val args = GlueArgParser.getResolvedOptions(sysArgs, Seq("JOB_NAME").toArray)
    Job.init(args("JOB_NAME"), glueContext, args.asJava)
    // @type: DataSource
    // @args: [database = "testdb", table_name = "testtable", additionalOptions = {"startingPosition": "TRIM_HORIZON", "inferSchema": "true"}, stream_type = kinesis]
    // @return: datasource0
    // @inputs: []
    val datasource0 = glueContext.getCatalogSource(database = "testdb", tableName = "testtable", redshiftTmpDir = "", transformationContext = "datasource0", additionalOptions = JsonOptions("""{"startingPosition": "TRIM_HORIZON", "inferSchema": "true"}""")).getDataFrame()
    // @type: DataSink
    // @args: [mapping = [("timestamp", "timestamp", "timestamp", "timestamp"), ("humidity", "double", "humidity", "double"), ("temperature", "double", "temperature", "double"), ("pressure", "double", "pressure", "double"), ("pitch", "double", "pitch", "double"), ("roll", "double", "roll", "double"), ("client_id", "string", "client_id", "string")], stream_batch_time = "100 seconds", stream_checkpoint_location = "s3://glue-streaming-test/sense/output/checkpoint/", connection_type = "s3", path = "s3://test-api-collector/sense/output/", format = "parquet", transformation_ctx = "datasink1"]
    // @return: datasink1
    // @inputs: [frame = datasource0]
    glueContext.forEachBatch(datasource0, (dataFrame: Dataset[Row], batchId: Long) => {
      val year: Int = Calendar.getInstance().get(Calendar.YEAR)
      val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
      val day: Int = Calendar.getInstance().get(Calendar.DATE)
      val hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      val minute: Int = Calendar.getInstance().get(Calendar.MINUTE)
      if (dataFrame.count() > 0) {
        val dynamicFrame = DynamicFrame(dataFrame, glueContext)
        val applyMapping = dynamicFrame.applyMapping(mappings = Seq(("timestamp", "timestamp", "timestamp", "timestamp"), ("humidity", "double", "humidity", "double"), ("temperature", "double", "temperature", "double"), ("pressure", "double", "pressure", "double"), ("pitch", "double", "pitch", "double"), ("roll", "double", "roll", "double"), ("client_id", "string", "client_id", "string")), caseSensitive = false, transformationContext = "applyMapping")
        val path = "s3://glue-streaming-test/sense/output" + "/ingest_year=" + "%04d".format(year) + "/ingest_month=" + "%02d".format(month) + "/ingest_day=" + "%02d".format(day) + "/ingest_hour=" + "%02d".format(hour) + "/"
        val datasink1 = glueContext.getSinkWithFormat(connectionType = "s3", options = JsonOptions(s"""{"path": "$path"}"""), transformationContext = "datasink1", format = "parquet").writeDynamicFrame(applyMapping)
      }
    }, JsonOptions("""{"windowSize" : "100 seconds", "checkpointLocation" : "s3://glue-streaming-test/sense/output/checkpoint/"}"""))
    Job.commit()
  }
}
