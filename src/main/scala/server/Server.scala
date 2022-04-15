package filesharer
package server

import java.io.{BufferedInputStream, BufferedOutputStream, Closeable, DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream, IOException, InputStream, ObjectOutputStream, OutputStream, PrintStream}
import java.net.{ServerSocket, Socket}
import javax.net.ssl.{SSLServerSocket, SSLServerSocketFactory}
import scala.io.BufferedSource
import library.Utils

import java.security.MessageDigest

class Server(controlPort: Int, dataPort: Int, storagePath: String, keyStorePath: String, keyStorePassword: String) {
  def connectControl(): SSLServerSocket = {
    System.setProperty("javax.net.ssl.keyStore", keyStorePath)
    System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword)
    val s = SSLServerSocketFactory.getDefault().createServerSocket(controlPort).asInstanceOf[SSLServerSocket]
    s.setEnabledProtocols(Utils.controlProtocols)
    s.setEnabledCipherSuites(Utils.controlCipherSuites)
    s
  }

  def readFileName(in: InputStream): String = {
    val dis = new DataInputStream(in)
    dis.readUTF()
  }

  def receive(dataSS: ServerSocket, controlIn: DataInputStream, controlOut: DataOutputStream): Unit = {
    val dataSocket = dataSS.accept()
    val dataIn = new DataInputStream(new BufferedInputStream(dataSocket.getInputStream))

    Utils.log("Server waiting to receive file name")
    Utils.wait(controlIn)
    val name = readFileName(controlIn)
    Utils.log(s"Server receiving file named $name")

    val receiveFile = new File(storagePath + name)
    val fos = new FileOutputStream(receiveFile)
    Utils.log("Server opened file")

    Utils.log("Server waiting to receive data")
    Utils.wait(dataIn)
    Utils.log("Server received data")

    val lengthRead = dataIn.transferTo(fos)
    fos.close()
    Utils.log("Server wrote to file")

    // tell client the data was properly received
    controlOut.writeLong(lengthRead)
    controlOut.flush()
    Utils.log("Server notified client of success")

    dataIn.close()
    dataSocket.close()
  }

  def run(): Unit = {
    var control: SSLServerSocket = null
    var data: ServerSocket = null
    var controlSocket: Socket = null

    try {
      control = connectControl()
      data = new ServerSocket(dataPort)

      Utils.log("Server running")

      controlSocket = control.accept()

      val cis = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream))
      val cos = new DataOutputStream(new BufferedOutputStream(controlSocket.getOutputStream))

      Utils.log("Server accepted a client")

      var clientConnected = true

      while (clientConnected) {
        Utils.log("\nServer waiting on client command")

        val mode = cis.readInt()

        Utils.log(s"Server received mode $mode from client")

        mode match {
          case 0 => receive(data, cis, cos)
          case 3 => clientConnected = false
          case _ => Utils.logError("Unknown command received from client")
        }
      }
    } catch {
      case io: IOException =>
        io.printStackTrace
        Utils.logError(s"Error connecting: ${io.getMessage}")
      case er: Exception =>
        er.printStackTrace
        Utils.logError(s"Unknown error: ${er.getMessage}")
    }
    finally {
      List(controlSocket, control, data).map(r => Utils.tryClose(r))
    }

    Utils.log("Server stopping execution")
  }
}

