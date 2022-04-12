package filesharer
package library

import java.io.InputStream

object Utils {
  // TODO: load from config
  val hostName = "localhost"
  val controlPort = 9999
  val dataPort = 9998
  
  def wait(in: InputStream): Unit = {
    while (in.available() < 1) { Thread.sleep(100) }
  }
  
  def log(s: String): Unit = {
    println(s)
  }
  
  def logError(s: String): Unit = {
    System.err.println(s)
  }
}
