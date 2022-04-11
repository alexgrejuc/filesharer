package filesharer
package client

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor

object Client {
  def connect(): Socket = {
    println("Client attempting connection")
    val s = new Socket(InetAddress.getByName("localhost"), 9999)
    println("Client connected")
    s
  }

  def disconnect(s: Socket): Unit = {
    val os = s.getOutputStream()

    // Tell the server the client is disconnecting
    os.write(3)
    os.flush()
    s.close()

    println("Client disconnected")
  }

  def send(fileName: String, s: Socket): Unit = {
    println("Client sending")
    val out = s.getOutputStream()

    out.write(0)
    out.flush()

    val sendFile = new File(fileName)
    val fis = new FileInputStream(sendFile)

    Encryptor.encryptTo(fis, out)

    fis.close()
    println("Client sent encrypted version of " + fileName)
  }

  def request(fileName: String, s: Socket) : Unit = {
    println("Client requesting")
    val out = s.getOutputStream()
    out.write(1)
    out.flush()

    val in = s.getInputStream()

    val receiveFile = new File("testfiles/client/decrypted.txt")
    val fos = new FileOutputStream(receiveFile)

    println("Client waiting")
    while (in.available() < 1) {Thread.sleep(100)}
    println("Client received file from server")

    Encryptor.decryptTo(in, fos)
    fos.close()
    println("Client wrote to file")
  }
}