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

  def writeAndHash(b: Byte, outputStream: DataOutputStream, messageDigest: MessageDigest): Unit = {
    outputStream.writeByte(b)
    messageDigest.update(b)
  }

  def receive(dataSS: ServerSocket, controlIn: DataInputStream, controlOut: DataOutputStream): Unit = {
    var dataIn: BufferedInputStream = null
    var fos: DataOutputStream = null

    try {
      val dataSocket = dataSS.accept()
      dataIn = new BufferedInputStream(dataSocket.getInputStream)

      Utils.log("Server waiting to receive file name")
      Utils.wait(controlIn)
      val name = readFileName(controlIn)
      Utils.log(s"Server receiving file named $name")

      val receiveFile = new File(storagePath + name)
      fos = new DataOutputStream(new FileOutputStream(receiveFile))
      Utils.log("Server opened file")

      Utils.log("Server waiting to receive data")
      Utils.wait(dataIn)
      Utils.log("Server received data")

      val sha256 = MessageDigest.getInstance("SHA-256")

      Iterator.continually(dataIn.read())
        .takeWhile(_ != -1)
        .foreach(i => writeAndHash(i.toByte, fos, sha256))

      fos.close()
      val hash = sha256.digest()

      controlOut.write(hash)
      controlOut.flush()
      Utils.log(s"Server sent hash to client: ${new String(hash)}")

      Utils.log("Server notified client of success")
    } catch {
      case ex: Exception => Utils.logError(s"Error receiving from client ${ex.getMessage}")
    }
    finally {
      List(dataIn, fos).map(r => Utils.tryClose(r))
    }
  }

  def handleClient(controlSocket: Socket, data: ServerSocket) = {
    val cis = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream))
    val cos = new DataOutputStream(new BufferedOutputStream(controlSocket.getOutputStream))
    var clientConnected = true

    while (clientConnected) {
      Utils.log("\nServer waiting on client command")

      val mode = try {
        cis.readInt()
      } catch {
        _ => -1
      }

      Utils.log(s"Server received mode $mode from client")

      mode match {
        case -1 =>
          Utils.log("Could not read command from client.")
          clientConnected = false
        case 0 => receive(data, cis, cos)
        case 3 =>
          Utils.log(s"Client disconnected.")
          clientConnected = false
        case _ =>
          Utils.logError("Unknown command received from client")
          clientConnected = false
      }
    }
  }

  def run(): Unit = {
    var control: SSLServerSocket = null
    var data: ServerSocket = null
    var controlSocket: Socket = null

    try {
      control = connectControl()
      data = new ServerSocket(dataPort)
      Utils.log("Server running")

      while (true) {
        controlSocket = control.accept()
        Utils.log("Server accepted a client")
        handleClient(controlSocket, data)
        controlSocket.close()
        }
      }
    catch {
      case io: IOException => Utils.logError(s"Error connecting: ${io.getMessage}")
      case er: Exception => Utils.logError(s"Unknown error: ${er.getMessage}")
    }
    finally {
      List(controlSocket, control, data).map(r => Utils.tryClose(r))
    }

    Utils.log("Server stopping execution")
  }
}

