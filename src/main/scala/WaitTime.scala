
import java.io._
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.{Base64, Date, UUID}
import java.util.logging.{Level, Logger}

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.http.client.methods.{CloseableHttpResponse, HttpEntityEnclosingRequestBase, HttpPost, HttpUriRequest}
import org.apache.http.conn.ssl.SSLContexts
import org.apache.http.entity.StringEntity
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions

import scala.collection.JavaConverters
import scala.util.parsing.json.JSON

/**
curl -XPUT 'localhost:9200/_template/wait' -H 'Content-Type: application/json' -d '{ "index_patterns": ["wait*"], "settings": { "number_of_shards": 1 }, "mappings" : { "wait" : { "properties" : { "timestamp" : { "type": "date", "format" : "epoch_millis" }, "name" : { "type" : "keyword" }, "wait" : { "type" : "long" } } } }}'
  */
class WaitTime() {
  def run() {
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
    val uuid = UUID.randomUUID()
    val date = LocalDateTime.now()

    val json = "{\"requestHeader\": {\"messageDateTime\": \"" + date + "\",\"requestMessageId\": \"" + uuid + "\"}, \"requestData\": { \"kind\": \"predict\" } }"

    val post = new HttpPost("https://sandbox.api.visa.com/visaqueueinsights/v1/queueinsights")
    val params = new StringEntity(json)
    post.addHeader("content-type", "application/json")
    post.addHeader("Authorization", ClientUtil.generateAuth())

    post.setEntity(params)

    val response = ClientUtil.send(post)

    println("Got Queue Insights message: " + response.getStatusLine.getStatusCode)

    val writer = new StringWriter()
    IOUtils.copy(response.getEntity.getContent, writer, "UTF-8")
    val json2 = writer.toString

    class CC[T] {
      def unapply(a: Any): Option[T] = Some(a.asInstanceOf[T])
    }

    object M extends CC[Map[String, Any]]
    object L extends CC[List[Any]]
    object ML extends CC[List[Map[String, Any]]]
    object S extends CC[String]
    object D extends CC[Double]
    object B extends CC[Boolean]

    val results = for {
      M(map) <- JSON.parseFull(json2)
      M(responseData) = map("responseData").asInstanceOf[Map[String, Any]]
      merchantList = responseData("merchantList")
    } yield {
      (merchantList)
      //    (name, wait)
    }

    val list = results.get.asInstanceOf[List[Any]]

    val m = for {
      elem <- list
      map = elem.asInstanceOf[Map[String, String]]
      wait = map("waitTime")
      name = map("name")
      date = new Date().getTime
    } yield {
      (name, wait, date)
    }

    val row = Row.fromSeq(m)

    val rdd = sparkSession.sparkContext.parallelize(m)
    val st = rdd.toDF()
      .withColumnRenamed("_1", "name")
      .withColumnRenamed("_2", "wait")
      .withColumnRenamed("_3", "timestamp")
    st
      .write
      .mode(SaveMode.Append)
      .format("es")
      .save("wait/wait")
  }
}

