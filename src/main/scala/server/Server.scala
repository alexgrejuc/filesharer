package filesharer
package server

import java.io.{FileOutputStream, FileInputStream, PrintStream, InputStream, File}
import java.net.ServerSocket
import scala.io.BufferedSource

object Server {
  def run(): Unit = {
    val server = new ServerSocket(9999)

    println("Server running")

    val s = server.accept()
    val in = s.getInputStream()

    val receiveFile = new File("testfiles/server/encrypted.txt")
    val fos = new FileOutputStream(receiveFile)

    println("Server opened file")

    while (in.available() < 1) { Thread.sleep(100) }
    val bytes = in.readNBytes(in.available())
    println("Server read from client")

    fos.write(bytes)
    fos.flush()
    fos.close()

    println("Server wrote to file")

    val sendFile = new File("testfiles/server/encrypted.txt")
    val fis = new FileInputStream(receiveFile)

    println("Server opened file for sending")

    val out = s.getOutputStream()
    out.write(fis.readAllBytes())
    out.flush()

    println("Server sent file")

    fis.close()
    s.close()

    println("Server stopping execution")
    server.close()
  }
}
