package filesharer
package keymanager

import library.Utils

import java.io.{BufferedInputStream, BufferedOutputStream, File, FileInputStream, FileOutputStream, IOException}
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

  // Generates a key given an algorithm and size in bits and writes it to a file
  def generateAndStoreKey(algorithm: String, size: Int, file: File): Boolean = {
    val key = generateKey(algorithm, size)
    var fos: BufferedOutputStream = null

    if (file.exists()) {
      Utils.logError(s"Key at ${file.getAbsolutePath} already exists. Delete it or provide another path.")
      return false
    }

    val result = try {
      fos = new BufferedOutputStream(new FileOutputStream(file))
      fos.write(key)
      fos.flush()
      Utils.log(s"Stored key at ${file.getAbsolutePath}")
      true
    } catch {
      case ex: Exception => Utils.logError(s"Could not store key:\n\t${ex.getMessage}")
        false
    }
    finally {
      Utils.tryClose(fos)
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
