


/**
 locator will return a 404 if nothing is found
 invalid status codes will return 200s
    val visaStoreId = 159339773

  */
class EnrichMerchant {
  def getStoreId(visaStoreId: String) : Map[String, String] = {
    import java.io._
    import java.time.LocalDateTime
    import java.util.UUID

    import org.apache.commons.io.IOUtils
    import org.apache.http.client.methods.HttpPost
    import org.apache.http.entity.StringEntity

    import scala.util.parsing.json.JSON
    val uuid = UUID.randomUUID()
    val date = LocalDateTime.now()

//    val merchantName = "Starbucks"
//    val json = "{ \"header\": {  \"messageDateTime\": \""+date+"\",  \"requestMessageId\": \""+uuid+"\",  \"startIndex\": \"0\" }, \"searchAttrList\": { \"merchantPostalCode\":\"95110\", \"merchantName\": \""+merchantName+"\",  \"merchantCountryCode\": \"840\",  \"distance\": \"10\",  \"distanceUnit\": \"M\" }, \"responseAttrList\": [  \"GNLOCATOR\" ], \"searchOptions\": {  \"maxRecords\": \"1\",  \"matchIndicators\": \"true\",  \"matchScore\": \"true\" }}"
    val json = "{ \"searchAttrList\": { \"visaStoreId\":\""+visaStoreId+"\" }, \"responseAttrList\": [ \"GNSTANDARD\" ], \"searchOptions\": {  \"maxRecords\": \"5\", \"matchIndicators\": \"true\", \"matchScore\": \"true\" }, \"header\": { \"requestMessageId\": \""+uuid+"\", \"startIndex\": \"0\", \"messageDateTime\": \""+date+"\" } }"

    val post = new HttpPost("https://sandbox.api.visa.com/merchantsearch/v1/search")
    post.addHeader("content-type", "application/json")
    post.addHeader("Authorization", ClientUtil.generateAuth())
    post.setEntity(new StringEntity(json))

    val response = ClientUtil.send(post)

    println("Merchant Locator: " + response.getStatusLine.getStatusCode)

    val writer = new StringWriter()
    IOUtils.copy(response.getEntity.getContent, writer, "UTF-8")
    val json2 = writer.toString

    class CC[T] {
      def unapply(a: Any): Option[T] = Some(a.asInstanceOf[T])
    }

    object M extends CC[Map[String, Any]]
    object MS extends CC[Map[String, String]]
    object L extends CC[List[Any]]

    val results = for {
      M(map) <- JSON.parseFull(json2)
      M(merchantSearchServiceResponse) = map("merchantSearchServiceResponse").asInstanceOf[Map[String, Any]]
      merchantLists = merchantSearchServiceResponse("response")

    } yield {
      merchantLists
    }
    println(results)
    val m = for {
      elem <- results
      M(ele) = elem.asInstanceOf[List[Any]].head
      MS(s) = ele("responseValues")
    } yield {
      Map("merchantStreetAddress"-> s("merchantStreetAddress"),
        "merchantCity" -> s("merchantCity"),
        "merchantState" -> s("merchantState"),
        "merchantPostalCode" -> s("merchantPostalCode"),
        "visaMerchantName" -> s("visaMerchantName"),
        "merchantCategoryCodeDesc" -> s("merchantCategoryCodeDesc").asInstanceOf[List[Any]].head.toString,
        "primaryMerchantCategoryCode" -> s("primaryMerchantCategoryCode")
      )
    }

    m.get
  }
}

