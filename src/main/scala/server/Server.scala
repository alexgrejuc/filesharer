package filesharer
package server

import java.io.{DataInputStream, File, FileInputStream, FileOutputStream, InputStream, PrintStream}
import java.net.ServerSocket
import scala.io.BufferedSource

object Server {
  // TODO: get from config file
  val storagePath = "testfiles/server/"

  def wait(in: InputStream): Unit = {
    while (in.available() < 1) { Thread.sleep(100) }
  }

  def readInfo(in: InputStream): (String, Int) = {
    val dis = new DataInputStream(in)

    val name = dis.readUTF()
    val size = dis.readInt()

    (name, size)
  }

  def run(): Unit = {
    val server = new ServerSocket(9999)

    println("Server running")

    val s = server.accept()
    val in = s.getInputStream()

    var clientConnected = true

    while (clientConnected) {
      wait(in)

      val dis = new DataInputStream(in)
      val mode = dis.readInt()

      println("Received mode from client: " + mode.toString())
      
      if (mode == 0) {
        wait(in)

        val (name, size) = readInfo(in)

        println(s"Server receiving file named $name of size: $size")

        val receiveFile = new File(storagePath + name)
        val fos = new FileOutputStream(receiveFile)

        println("Server opened file")

        wait(in)

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

        val dis = new DataInputStream(in)
        val fileName = dis.readUTF()

        val sendFile = new File(storagePath + fileName)
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
