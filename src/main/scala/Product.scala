
import java.text.SimpleDateFormat
import java.util.logging.{Level, Logger}

import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions

/**
  * USAGE:
  *
Build the template:

curl -XPUT 'localhost:9200/_template/product' -H 'Content-Type: application/json' -d '{ "index_patterns": ["product*"], "settings": { "number_of_shards": 1 }, "mappings" : { "product" : { "properties" : { "Country" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "CustomerID" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Description" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "InvoiceDate" : { "type" : "date", "format" : "epoch_millis" }, "InvoiceNo" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Quantity" : { "type" : "long" }, "StockCode" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "UnitPrice" : { "type" : "long" } } } } }'


  curl -XPUT 'localhost:9200/_template/product' -H 'Content-Type: application/json' -d '{ "index_patterns": ["product*"], "settings": { "number_of_shards": 1 }, "mappings" : { "product" : { "properties" : { "Country" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "CustomerID" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Description" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "InvoiceDate" : { "type" : "date", "format" : "epoch_millis" }, "InvoiceNo" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Quantity" : { "type" : "long" }, "StockCode" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "UnitPrice" : { "type" : "long" } } } } }'
  */
class Product() {
  val jvmOpts = ""
  val conf: SparkConf = new SparkConf()
    .setAppName("Insights")
    .set("spark.driver.extraJavaOptions", jvmOpts)
    .set("spark.executor.extraJavaOptions", jvmOpts)
    .set("spark.executor.memory", "10g")
    .set(ConfigurationOptions.ES_NODES, "localhost:9200")
    .set(ConfigurationOptions.ES_WRITE_OPERATION, ConfigurationOptions.ES_OPERATION_INDEX) //use index, not create, if you don't specify ids
    .set(ConfigurationOptions.ES_BATCH_SIZE_BYTES, "2mb")
    .set(ConfigurationOptions.ES_BATCH_SIZE_ENTRIES, "2000")
    .set(ConfigurationOptions.ES_BATCH_WRITE_REFRESH, "false")
    .set(ConfigurationOptions.ES_INDEX_AUTO_CREATE, "true")
    .set(ConfigurationOptions.ES_BATCH_WRITE_RETRY_COUNT, "15")
    .set("mapred.map.tasks.speculative.execution", "false") //this should be off?
    .set("mapred.reduce.tasks.speculative.execution", "false") //this should be off?
    .set("spark.speculation", "false") //this should be off?
    .set("script.groovy.sandbox.enabled", "false")

  val config = new Configuration
  config.set("dfs.client.block.write.replace-datanode-on-failure.policy", "ALWAYS")

  val sparkSession: SparkSession = SparkSession.builder.master("local").config(conf).getOrCreate()
  sparkSession.sparkContext.setLogLevel("ERROR")
  import sparkSession.implicits._


  //InvoiceNo,StockCode,Description,Quantity,InvoiceDate,UnitPrice,CustomerID,Country
  val schema = StructType(Seq(
    StructField("InvoiceNo", StringType, true),
    StructField("StockCode", StringType, true),
    StructField("Description", StringType, true),
    StructField("Quantity", StringType, true),
    StructField("InvoiceDate", StringType, true),
    StructField("UnitPrice", StringType, true),
    StructField("CustomerID", StringType, true),
    StructField("Country", StringType, true)
  ))

  Logger.getLogger("org").setLevel(Level.INFO)
  Logger.getLogger("com").setLevel(Level.INFO)

  def dateFunc: (String => Long) = {
    s => {
      val format = "MM/dd/yy HH:mm"
      val sdf = new SimpleDateFormat(format)
      if (s != null) sdf.parse(s).getTime + (31536000000L * 7) - 10368000000L else 0
    }
  }
  val dateUdf = udf(dateFunc)

  val tax = sparkSession.read.format("csv")
    .option("delimiter", ",")
    .option("header", "true")
    .schema(schema)
    .load("/Users/henneberger/GitProjects/sba-hack/retail.csv")
    .withColumn("InvoiceDate", dateUdf('InvoiceDate))
    .withColumnRenamed("Description", "Items")
    .withColumn("TotalPrice", 'UnitPrice * 'Quantity)

  tax
    .write
    .mode(SaveMode.Append)
    .format("es")
    .save("product-sba/product")
}