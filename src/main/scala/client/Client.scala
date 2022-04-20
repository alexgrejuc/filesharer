package filesharer
package client

import java.io.{BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream, InputStream, ObjectInputStream, OutputStream, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import encryptor.Encryptor
import library.Utils

import javax.net.ssl.{SSLSocket, SSLSocketFactory}

class Client(hostName: String, controlPort: Int, dataPort: Int, trustStorePath: String, trustStorePassword: String, secretKey: Array[Byte]) {
  var controlSocket: SSLSocket = null

  def connect(): Boolean = {
    val info = s"server at $hostName with control port $controlPort and data port $dataPort"

    try {
      System.setProperty("javax.net.ssl.trustStore", trustStorePath)
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword)
      controlSocket = SSLSocketFactory.getDefault().createSocket(hostName, controlPort).asInstanceOf[SSLSocket]
      controlSocket.setEnabledProtocols(Utils.controlProtocols)
      controlSocket.setEnabledCipherSuites(Utils.controlCipherSuites)
      Utils.log(s"Client connected to $info")
      Utils.log(s"Using trust store at $trustStorePath")
      Utils.log("")
      true
    } catch {
      case ex: Exception => Utils.logError(s"Client could not connect to $info: ${ex.getMessage}")
        Utils.tryClose(controlSocket)
        false
    }
  }

  def notifyDisconnect(os: OutputStream): Unit = {
    val dos = new DataOutputStream(os)
    dos.writeInt(Utils.DISCONNECT)
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
    cos.writeInt(Utils.SEND)

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

    val hash = Encryptor.encryptAndHash(fis, dos, secretKey)
    dos.close()
    fis.close()
    Utils.log(s"Sent file.")

    val cis = new BufferedInputStream(controlSocket.getInputStream)
    val receivedHash = cis.readNBytes(32)

    if (receivedHash.sameElements(hash)) {
      Utils.log("Client notified that server received file with matching hash")
    }
    else {
      Utils.logError(s"Hash mismatch.\nClient: ${new String(hash)}\nServer: ${new String(receivedHash)}")
    }
  }

  def notifyRequest(fileName: String, control: OutputStream): Unit = {
    val cos = new DataOutputStream(control)

    cos.writeInt(Utils.REQUEST)

    cos.writeUTF(fileName)
    cos.flush()
    Utils.log("Client notified server it is requesting")
  }

  // Requests a file from the server, saves it to a local file, compares actual and expected
  // Steps:
  //    Tell server that the client would like to request
  //    Tell server the file name
  //    Receive, hash, and write the file
  //    Receive the expected hash from the server
  //    Compare actual and expected hashes
  def request(fileName: String, toPath: String): Unit = {
    Utils.log(s"Client requesting ${fileName}")

    var dataSocket: Socket = null
    var fos: FileOutputStream = null

    try {
      dataSocket = connectDataSocket()
      val dis = dataSocket.getInputStream() // encryptor writes to this using its own buffer
      val cos = controlSocket.getOutputStream() // control commands should be sent immediately, so don't buffer
      val cis = new BufferedInputStream(controlSocket.getInputStream())

      notifyRequest(fileName, cos)

      // check that the file actually exists (size >= 0)
      val ddis = new DataInputStream(dis)
      val size = ddis.readLong()

      if (size >= 0) {
        val file = new File(toPath)
        fos = new FileOutputStream(file)

        val actualHash = Encryptor.hashAndDecrypt(dis, fos, secretKey)
        val expectedHash = cis.readNBytes(32)

        if (actualHash.sameElements(expectedHash)) {
          Utils.log("Received data hashes to expected value.")
        }
        else {
          Utils.logError("Received data does not hash to expected value. It is potentially unsafe.")
        }
      }
      else {
        Utils.log(s"Client notified that file $fileName does not exist on the server.")
      }
    }
    catch {
      case ex: Exception => Utils.logError(s"Error requesting file: ${ex.getMessage}")
    }
    finally {
      List(dataSocket, fos).map(r => Utils.tryClose(r))
    }
  }

  // Asks server to delete file with name fileName
  def delete(fileName: String): Unit = {
    Utils.log(s"Client deleting ${fileName}")

    try {
      val cis = new DataInputStream(controlSocket.getInputStream)
      val cos = new DataOutputStream(controlSocket.getOutputStream)

      cos.writeInt(Utils.DELETE)
      cos.writeUTF(fileName)

      val length = cis.readLong()

      if (length >= 0) {
        Utils.log(s"Client notified that server deleted $fileName of size $length bytes")
      }
      else {
        Utils.log(s"Client notified that server could not delete $fileName. Perhaps the name is incorrect.")
      }
    }
    catch {
      case ex: Exception => Utils.logError(s"Error deleting file: ${ex.getMessage}")
    }
  }

  def list(): Unit = {
    def display(filesAndSizes: List[(String, Long)]): Unit = {
      if (filesAndSizes.length > 0) {
        val fileNameLabel = "File Name"
        val sizeLabel = "Size (bytes)"
        val maxDigitsOfLong = 19

        // determine padding length
        val longestFileName = filesAndSizes.maxBy(p => p._1.length)._1.length
        val longest = longestFileName.max(fileNameLabel.length)

        // print header and a horizontal bar followed by the newline-separated file info
        val header = fileNameLabel.padTo(longest, ' ') + " | " + sizeLabel.padTo(maxDigitsOfLong, ' ')
        val bar = "-" * header.length
        Utils.log("\n" + header)
        Utils.log(bar)
        filesAndSizes.map(p => Utils.log(p._1.padTo(longestFileName, ' ') + " | " + p._2))
        Utils.log("")
      }
      else {
        Utils.log("No files stored on the server.")
      }
    }

    Utils.log("Client requesting list of saved files.")

    try {
      val cos = new DataOutputStream(controlSocket.getOutputStream)
      cos.writeInt(Utils.LIST)

      // ObjectInputStream will block until the ObjectOutputStream on the other end is created
      // Here is must be created after writing the LIST command to prevent client and server both waiting on each other
      val cis = new ObjectInputStream(controlSocket.getInputStream)

      val files = cis.readObject().asInstanceOf[Option[List[(String, Long)]]]

      files match {
        case Some(fs) => display(fs)
        case _ => Utils.log("The server failed to retrieve file names.")
      }
    } catch {
      case ex: Exception => Utils.logError(s"Error listing files: ${ex.getMessage}")
    }
  }

  // Sends files to the server given an array of client-side file paths
  def sendFiles(filePaths: Array[String]): Unit = {
    for (fp <- filePaths) {
      val f = new File(fp)
      if (f.exists() && !f.isDirectory() && f.canRead()) {
        send(f)
      }
      else {
        Utils.logError(s"Could not access file $fp")
      }
      Utils.log("")
    }
  }

  // Requests files from server and saves them to the specified location
  // filesAndPaths must contain a file name followed by a file path to save to
  def requestFiles(filesAndPaths: Array[String]): Unit = {
    if (filesAndPaths.length % 2 == 0) {
      for (i <- 0 until (filesAndPaths.length / 2)) {
        request(filesAndPaths(i * 2), filesAndPaths((i * 2) + 1))
      }
    }
    else {
      Utils.logError("Invalid arguments: each requested file needs a corresponding path to be saved to.")
    }
  }
}
