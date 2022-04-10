package filesharer
package client

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor

object Client {
  def send(fileName: String): Unit = {
    println("Client attempting connection")

    val s = new Socket(InetAddress.getByName("localhost"), 9999)

    println("Client connected")
    
    val sendFile = new File(fileName)
    val fis = new FileInputStream(sendFile)
    
    val out = s.getOutputStream()

    Encryptor.encryptTo(fis, out)
    out.flush()

    println("Client sent encrypted version of " + fileName)

    val in = s.getInputStream()

    val receiveFile = new File("testfiles/client/decrypted.txt")
    val fos = new FileOutputStream(receiveFile)

    println("Client waiting")
    while (in.available() < 1) {Thread.sleep(100)}
    println("Client received file from server")

    Encryptor.decryptTo(in, fos)
    fos.close()
    println("Client wrote to file")

    println("Client finishing execution")
    s.close()
  }
}