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

  def writeAndHash(b: Byte, outputStream: DataOutputStream, messageDigest: MessageDigest): Unit = {
    outputStream.writeByte(b)
    messageDigest.update(b)
  }

  // Receives data from a client, saves it to a file, and responds to the client with a hash of what it received.
  def receive(dataSS: ServerSocket, controlIn: DataInputStream, controlOut: DataOutputStream): Unit = {
    var dataIn: BufferedInputStream = null
    var fos: DataOutputStream = null

    try {
      val dataSocket = dataSS.accept()
      dataIn = new BufferedInputStream(dataSocket.getInputStream)

      Utils.log("Server waiting to receive file name")
      Utils.wait(controlIn)
      val fileName = controlIn.readUTF
      Utils.log(s"Server receiving file named $fileName")

      val receiveFile = new File(storagePath + fileName)
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

  // Sends requested data to client as well as its hash.
  // Steps:
  //        Receive file name from client
  //        Read file from disk, hash it, and send it to the client
  //        Send client the intended hash
  def send(dataSS: ServerSocket, controlIn: DataInputStream, controlOut: DataOutputStream): Unit = {
    var dataSocket: Socket = null
    var fis: FileInputStream = null

    try {
      val fileName = controlIn.readUTF
      Utils.log(s"Server sending file $fileName to client.")

      dataSocket = dataSS.accept()
      val dos = new DataOutputStream(dataSocket.getOutputStream)

      val file = new File(storagePath + fileName)
      fis = new FileInputStream(file)
      val sha256 = MessageDigest.getInstance("SHA-256")

      // read file with buffer, hash, and send buffer contents to client
      val buffer = new Array[Byte](8192)
      var count = fis.read(buffer)

      while (count != -1) {
        dos.write(buffer, 0, count)
        dos.flush()
        sha256.update(buffer, 0, count)
        count = fis.read(buffer)
      }

      Utils.log("Sent file to client.")
      dos.close()

      val hash = sha256.digest()
      controlOut.write(hash)

      Utils.log("Sent hash to client")
    } catch {
      case ex: Exception => Utils.logError(s"Error sending to client: ${ex.getMessage}")
    }
    finally {
      List(dataSocket, fis).map(r => Utils.tryClose(r))
    }
  }

  // Deletes a file in server storage with name specified by the client.
  // Tells client size in bytes of the deleted file or -1 if the operation failed
  def delete(controlIn: DataInputStream, controlOut: DataOutputStream): Unit = {
    var length: Long = -1

    try {
      val fileName = controlIn.readUTF()
      val file = new File(storagePath + fileName)

      if (file.exists() && file.isFile) {
        length = file.length()
        file.delete()
        Utils.log(s"${file.getAbsolutePath} deleted")
      }
      else {
        Utils.log(s"Not able to delete ${file.getAbsolutePath} because it does not exist or is not a file")
      }
    } catch {
      case ex: Exception => Utils.logError(s"Error deleting file: ${ex.getMessage}")
    }
    finally {
      controlOut.writeLong(length)
    }
  }


  def handleClient(controlSocket: Socket, data: ServerSocket) = {
    val cis = new DataInputStream(controlSocket.getInputStream)
    val cos = new DataOutputStream(controlSocket.getOutputStream)
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
        case Utils.SEND => receive(data, cis, cos)
        case Utils.REQUEST => send(data, cis, cos)
        case Utils.DELETE => delete(cis, cos)
        case Utils.DISCONNECT =>
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
