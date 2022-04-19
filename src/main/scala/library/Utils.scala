package filesharer
package library

import java.io.{Closeable, IOException, InputStream}

object Utils {
  // TODO: load from config
  val hostName = "localhost"
  val controlPort = 9999
  val dataPort = 9998
  val controlProtocols = Array("TLSv1.3")
  val controlCipherSuites = Array("TLS_AES_128_GCM_SHA256")

  val SEND = 0
  val REQUEST = 1
  val DELETE = 2
  val LIST = 3 
  val DISCONNECT = 4
  
  def wait(in: InputStream): Unit = {
    while (in.available() < 1) { Thread.sleep(100) }
  }
  
  def log(s: String): Unit = {
    println(s)
  }
  
  def logError(s: String): Unit = {
    System.err.println(s)
  }

  def tryClose(c: Closeable): Unit = {
    if (c != null) {
      try {
        c.close()
      } catch {
        case ioe: IOException => Utils.logError(s"Error closing resource: ${ioe.printStackTrace}")
      }
    }
  }
}
