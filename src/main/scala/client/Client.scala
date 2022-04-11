package filesharer
package client

import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor

object Client {
  def wait(in: InputStream): Unit = {
    while (in.available() < 1) {
      Thread.sleep(100)
    }
  }

  def notifyDisconnect(os: OutputStream): Unit = {
    os.write(3)
    os.flush()
  }

  def notifySend(os: OutputStream): Unit = {
    os.write(0)
    os.flush()
  }

  def notifyRequest(os: OutputStream): Unit = {
    os.write(1)
    os.flush()
  }

  def connect(): Socket = {
    println("Client attempting connection")
    val s = new Socket(InetAddress.getByName("localhost"), 9999)
    println("Client connected")
    s
  }

  def disconnect(s: Socket): Unit = {
    val os = s.getOutputStream()
    notifyDisconnect(os)
    s.close()
    println("Client disconnected")
  }

  def send(fileName: String, s: Socket): Unit = {
    println("Client sending")
    val os = s.getOutputStream()
    notifySend(os)

    val sendFile = new File(fileName)
    val fis = new FileInputStream(sendFile)

    Encryptor.encryptTo(fis, os)

    fis.close()
    println("Client sent encrypted version of " + fileName)
  }

  def request(fileName: String, s: Socket) : Unit = {
    println("Client requesting")
    val os = s.getOutputStream()

    notifyRequest(os)

    val is = s.getInputStream()

    val receiveFile = new File(fileName)
    receiveFile.createNewFile()
    val fos = new FileOutputStream(receiveFile)

    println("Client waiting")
    wait(is)
    println("Client received file from server")

    Encryptor.decryptTo(is, fos)
    fos.close()
    println("Client wrote to file")
  }
}
