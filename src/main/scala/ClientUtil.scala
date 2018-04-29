import java.io.{File, FileInputStream}
import java.security.KeyStore
import java.util.Base64

import org.apache.http.client.methods.{CloseableHttpResponse, HttpEntityEnclosingRequestBase, HttpRequestBase}
import org.apache.http.conn.ssl.SSLContexts

/**
  * Created by henneberger on 4/28/18.
  */
object ClientUtil {

  def send(message: HttpRequestBase): CloseableHttpResponse = {

    import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
    import org.apache.http.impl.client.HttpClients;

    // Load client certificate into key store
    val keystore  = KeyStore.getInstance("jks")
    val cl = getClass().getClassLoader()
    val file = new File("/Users/henneberger/GitProjects/sba-hack/myProject_keyAndCertBundle.jks")
    val instream = new FileInputStream(file)
    try {
      keystore.load(instream, "testing".toCharArray())
    } finally {
      instream.close()
    }

    val sslcontext = SSLContexts.custom()
      .loadKeyMaterial(keystore,
        "testing".toCharArray)
      .build()

    val sr =  { "TLSv1.2" }
    // Allow TLSv1.2 protocol only
    val sslSocketFactory = new SSLConnectionSocketFactory(sslcontext)

    val httpClient = HttpClients.custom()
      .setSSLSocketFactory(sslSocketFactory).build()


    httpClient.execute(message)
  }

  def generateAuth(): String = {
    val username = "9ZSTFTAH6J1L0E4GBOF621NP7rryPGj0ik53EEVcKoT4Vxss8"
    val password = "GlvUcNlTHjqV455VnU8IAhZyjI48Hu"
    val toEncode = username + ":" + password
    val b64 = new String(Base64.getEncoder.encode(toEncode.getBytes))

    "Basic " + b64
  }
}
