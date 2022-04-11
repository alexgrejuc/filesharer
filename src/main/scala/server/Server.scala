package filesharer
package server

import java.io.{DataInputStream, File, FileInputStream, FileOutputStream, InputStream, PrintStream}
import java.net.ServerSocket
import scala.io.BufferedSource

object Server {
  def run(): Unit = {
    val server = new ServerSocket(9999)

    println("Server running")

    val s = server.accept()
    val in = s.getInputStream()

    var clientConnected = true

    while (clientConnected) {
      while (in.available() < 1) {Thread.sleep(100)}
      val mode = in.read()
      println("Received mode from client: " + mode.toString())
      
      if (mode == 0) {
        while (in.available() < 1) {Thread.sleep(100)}

        // wrap in DataStream to read entire int
        val dis = new DataInputStream(in)
        val size = dis.readInt()

        println("Server receiving file of size: " + size)

        val receiveFile = new File("testfiles/server/encrypted.txt")
        val fos = new FileOutputStream(receiveFile)

        println("Server opened file")

        while (in.available() < 1) {Thread.sleep(100)}

        println("Server received data")

        val bytes = in.readNBytes(size)
        println("Server read from client")

        fos.write(bytes)
        fos.flush()
        fos.close()

        println("Server wrote to file")
      }
      else if (mode == 1) {
        println("Server sending")

        val sendFile = new File("testfiles/server/encrypted.txt")
        val fis = new FileInputStream(sendFile)

        println("Server opened file for sending")

        val out = s.getOutputStream()
        out.write(fis.readAllBytes())
        out.flush()

        println("Server sent file")

        fis.close()
      }
      else if (mode == 2) {
        // todo: list files
      }
      else if (mode == 3) {
        s.close()
        clientConnected = false
        println("Client disconnected")
      }
    }

    println("Server stopping execution")
    server.close()
  }
}
