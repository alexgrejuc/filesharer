package filesharer
package library

import java.io.{Closeable, IOException, InputStream}

/*
 * Utility functions and variables for use across the FileSharer application
 */
object Utils {
  // Common variables between client and server for setting up TLS 1.3 control socket
  val controlProtocols = Array("TLSv1.3")
  val controlCipherSuites = Array("TLS_AES_128_GCM_SHA256")

  // The client sends these commands and the server interprets them
  val SEND = 0
  val REQUEST = 1
  val DELETE = 2
  val LIST = 3 
  val DISCONNECT = 4

  // Wrapper functions for logging used to allow later potential refactoring

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
