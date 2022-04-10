package filesharer
package server

import java.io.{FileOutputStream, PrintStream, InputStream, File}
import java.net.ServerSocket
import scala.io.BufferedSource

object Server {
  def run(): Unit = {
    val server = new ServerSocket(9999)

    println("Server running")

    val s = server.accept()
    val in = s.getInputStream()

    println("Received communication")

    val file = new File("testfiles/server/encrypted.txt")
    val fs = new FileOutputStream(file)

    println("Server opened file")
    
    val bytes = in.readAllBytes()
    println("Server read from client")
    fs.write(bytes)
    fs.flush()
    fs.close()

    println("Server closed file")
    
    s.close()

    println("Server stopping execution")
    server.close()
  }
}
