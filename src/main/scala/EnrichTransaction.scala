
import java.io._
import java.time.LocalDateTime
import java.util.{Date, UUID}

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions

import scala.util.parsing.json.JSON

/**

  */
class EnrichTransaction() {
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

    val builder = new URIBuilder("https://sandbox.api.visa.com/visadirect/v1/transactionquery")
    builder.addParameter("acquiringBIN", "408999")
//    builder.addParameter("transactionIdentifier", "587010322176105")
    builder.addParameter("rnn", "707614949341")

    println(builder.build())
    val get = new HttpGet(builder.build())

    get.addHeader("content-type", "application/json")
    get.addHeader("Authorization", ClientUtil.generateAuth())


    val response = ClientUtil.send(get)

    println("Got Queue Insights message: " + response.getStatusLine.getStatusCode)

    val writer = new StringWriter()
    IOUtils.copy(response.getEntity.getContent, writer, "UTF-8")
    val json2 = writer.toString



    println(json2)
  }
}

