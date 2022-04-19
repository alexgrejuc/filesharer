package filesharer
package keymanager

import library.Utils

import java.io.{BufferedInputStream, BufferedOutputStream, BufferedReader, File, FileInputStream, FileOutputStream, FileReader, IOException}
import javax.crypto.KeyGenerator

/*
 * Generates, stores and retrieves encryption keys from files
*/
object KeyManager {
  // Generates a key given an algorithm and size in bits
  def generateKey(algorithm: String, size: Int): Array[Byte] = {
    val gen = KeyGenerator.getInstance(algorithm)
    gen.init(size)
    val key = gen.generateKey
    key.getEncoded
  }

  // Generates a key given an algorithm and size in bits
  // Writes it to the location specified in the given client configuration file
  def generateAndStoreKey(algorithm: String, size: Int, clientConfigFile: File): Boolean = {
    val key = generateKey(algorithm, size)
    var configFis: BufferedReader = null
    var keyFos: FileOutputStream = null

    val result = try {
      configFis = new BufferedReader(FileReader(clientConfigFile))
      val keyPath = configFis.readLine()
      val keyFile = new File(keyPath)

      if (keyFile.exists()) {
        Utils.logError(s"Key at ${keyFile.getAbsolutePath} already exists. Delete it or provide another path.")
        false
      }
      else {
        keyFos = new FileOutputStream(keyFile)
        keyFos.write(key)
        keyFos.flush()
        Utils.log(s"Stored key at ${keyFile.getAbsolutePath}")
        true
      }
    } catch {
      case ex: Exception => Utils.logError(s"Could not store key:\n\t${ex.getMessage}")
        false
    }
    finally {
      Utils.tryClose(keyFos)
    }

    result
  }

  def readKey(file: File): Option[Array[Byte]] = {
    var fis: BufferedInputStream = null

    val key = try {
      fis = new BufferedInputStream(new FileInputStream(file))
      fis.readAllBytes
    } catch {
      case ex: Exception => Utils.logError(s"Could not read key:\n\t${ex.getMessage}")
        null
    }
    finally {
      Utils.tryClose(fis)
    }

    if (key == null) {
      None
    }
    else if (key.length != 16) {
      Utils.logError(s"Found ${key.length} byte key. Key must be 16 bytes long.")
      None
    }
    else {
      Some(key)
    }
  }
}
