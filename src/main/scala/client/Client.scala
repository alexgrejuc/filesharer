package filesharer
package client

import java.io.{BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream, InputStream, ObjectInputStream, OutputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor
import library.Utils

import javax.net.ssl.{SSLSocket, SSLSocketFactory}

class Client(hostName: String, controlPort: Int, dataPort: Int, trustStorePath: String, trustStorePassword: String) {
  var controlSocket: SSLSocket = null

  def connect(): Unit = {
    System.setProperty("javax.net.ssl.trustStore", trustStorePath)
    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword)
    controlSocket = SSLSocketFactory.getDefault().createSocket(hostName, controlPort).asInstanceOf[SSLSocket]
    controlSocket.setEnabledProtocols(Utils.controlProtocols)
    controlSocket.setEnabledCipherSuites(Utils.controlCipherSuites)
    Utils.log("Client connected to server")
  }
  
  def notifyDisconnect(os: OutputStream): Unit = {
    val dos = new DataOutputStream(new BufferedOutputStream(os))
    dos.writeInt(3)
    dos.flush()
  }

  def disconnect(): Unit = {
    val os = controlSocket.getOutputStream()
    notifyDisconnect(os)
    os.close()
    Utils.log("Client disconnected")
  }

  def connectDataSocket(): Socket = {
    new Socket(InetAddress.getByName(hostName), dataPort)
  }
  
  def notifySend(fileName: String, control: OutputStream): Unit = {
    val cos = new DataOutputStream(control)

    Utils.log("Client notifying server that it would like to send")
    cos.writeInt(0) // tell the server the client would like to send a file
    Utils.log("Client sent command")

    cos.writeUTF(fileName)
    cos.flush()
    Utils.log("Client notified server it is sending")
  }

  def send(file: File): Unit = {
    Utils.log("Client sending")
    val dataSocket = connectDataSocket()
    val dos = new BufferedOutputStream(dataSocket.getOutputStream())
    val cos = new BufferedOutputStream(controlSocket.getOutputStream())

    notifySend(file.getName(), cos)

    val sendFile = new File(file.getAbsolutePath())
    val fis = new BufferedInputStream(new FileInputStream(sendFile))

    val sentLength = Encryptor.encryptTo(fis, dos)
    dos.close()
    fis.close()
    Utils.log(s"Sent $sentLength bytes")

    val dis = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream))
    val receivedLength = dis.readLong()

    if (receivedLength == sentLength) {
      Utils.log(s"Client notified that server received all $receivedLength bytes")
    }
    else {
      Utils.logError("Client failed to send")
    }
  }
}
