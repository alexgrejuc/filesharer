package filesharer
package client

import java.io.{BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream, InputStream, ObjectInputStream, OutputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor
import library.Utils

object Client {
  def connect(host: String, controlPort: Int): Socket = {
    val controlSocket = new Socket(InetAddress.getByName(host), controlPort)
    Utils.log("Client connected to server")
    controlSocket
  }
  
  def notifyDisconnect(os: OutputStream): Unit = {
    val dos = new DataOutputStream(new BufferedOutputStream(os))
    dos.writeInt(3)
    dos.flush()
  }

  def disconnect(s: Socket): Unit = {
    val os = s.getOutputStream()
    notifyDisconnect(os)
    s.close()
    Utils.log("Client disconnected")
  }

  def connectDataSocket(host: String, port: Int): Socket = {
    new Socket(InetAddress.getByName(host), port)
  }
  
  def notifySend(fileName: String, data: OutputStream, control: OutputStream): Unit = {
    val dos = new DataOutputStream(data)
    val cos = new DataOutputStream(control)

    cos.writeInt(0) // tell the server the client would like to send a file
    cos.flush()
    dos.writeUTF(fileName)
    dos.flush()
  }

  def send(file: File, controlSocket: Socket, host: String, dataPort: Int): Unit = {
    Utils.log("Client sending")
    val dataSocket = connectDataSocket(host, dataPort)
    val dos = new BufferedOutputStream(dataSocket.getOutputStream())
    val cos = new BufferedOutputStream(controlSocket.getOutputStream())

    notifySend(file.getName(), dos, cos)

    val sendFile = new File(file.getAbsolutePath())
    val fis = new BufferedInputStream(new FileInputStream(sendFile))

    val sentLength = Encryptor.encryptTo(fis, dos)
    dos.close()
    fis.close()
    Utils.log(s"Sent $sentLength bytes")

    val dis = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream))
    Utils.wait(dis)
    val receivedLength = dis.readLong()

    if (receivedLength == sentLength) {
      Utils.log(s"Client notified that server received all $receivedLength bytes")
    }
    else {
      Utils.logError("Client failed to send")
    }
  }
}
