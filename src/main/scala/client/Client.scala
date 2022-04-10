package filesharer
package client

import java.io.{File, FileInputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor

object Client {
  def run(): Unit = {
    val s = new Socket(InetAddress.getByName("localhost"), 9999)
    lazy val in = new BufferedSource(s.getInputStream()).getLines()
    val out = new PrintStream(s.getOutputStream())

    out.println("Client is running")
    out.flush()
    println("Received: " + in.next())

    s.close()
  }

  def send(fileName: String): Unit = {
    println("Client attempting connection")

    val s = new Socket(InetAddress.getByName("localhost"), 9999)

    println("Client connected")
    
    val f = new File(fileName)
    val fis = new FileInputStream(f)
    
    val out = s.getOutputStream()

    Encryptor.encryptTo(fis, out)

    println("Client sent encrypted version of " + fileName)
    println("Client finishing execution")
    s.close()
  }
}