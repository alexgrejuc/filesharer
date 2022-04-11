package filesharer
package client

import java.io.{DataOutputStream, File, FileInputStream, FileOutputStream, InputStream, OutputStream, PrintStream}
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
    val dos = new DataOutputStream(os)
    dos.writeInt(3)
    dos.flush()
  }

  def notifySend(fileName: String, os: OutputStream): Unit = {
    val dos = new DataOutputStream(os)
    dos.writeInt(0) // tell the server the client would like to send a file
    dos.writeUTF(fileName)
    dos.flush()
  }

  def notifyRequest(fileName:String, os: OutputStream): Unit = {
    val dos = new DataOutputStream(os)
    dos.writeInt(1)
    dos.writeUTF(fileName)
    dos.flush()
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

  def send(fileName: String, filePath: String, s: Socket): Unit = {
    println(s"Client sending file")
    val os = s.getOutputStream()
    
    notifySend(fileName, os)

    val sendFile = new File(filePath)
    val fis = new FileInputStream(sendFile)

    Encryptor.encryptTo(fis, os)

    fis.close()
    println("Client sent encrypted version of " + filePath)
  }

  def request(fileName: String, filePath: String, s: Socket) : Unit = {
    println("Client requesting")
    val os = s.getOutputStream()
    
    notifyRequest(fileName, os)

    val is = s.getInputStream()

    val receiveFile = new File(filePath)
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
