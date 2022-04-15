package filesharer
package encryptor

import java.io.{DataOutputStream, File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import library.Utils

import java.security.MessageDigest

object Encryptor {
  // Encrypts data from an input stream to an output stream given a cipher in encryption mode.
  // Returns a SHA-256 hash of the encrypted data.
  def encryptAndHashWith(is: InputStream, os: OutputStream, cipher: Cipher): Array[Byte] = {
    val sha256 = MessageDigest.getInstance("SHA-256")

    val buffer = new Array[Byte](8192)
    var count = is.read(buffer)

    // Stream encrypt using a buffer instead of loading it all in memory
    // Steps: read data, encrypt it, hash encrypted data, write encrypted data
    while (count != -1) {
      val encrypted = cipher.update(buffer, 0, count)
      os.write(encrypted)
      os.flush()
      sha256.update(encrypted)
      count = is.read(buffer)
    }

    val encrypted = cipher.doFinal()
    os.write(encrypted)
    os.flush()
    sha256.update(encrypted)

    sha256.digest()
  }

  // Encrypts data from an input stream to an output stream with AES/CBC/PKCS5PADDING
  // Returns a SHA-256 hash of the encrypted data.
  def encryptAndHash(is: InputStream, os: OutputStream, initVector: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val iv = new IvParameterSpec(initVector)
    val skeySpec = new SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)

    encryptAndHashWith(is, os, cipher)
  }

  // Decrypts data from an input stream to an output stream given a cipher in decryption mode.
  // Returns a SHA-256 hash of the encrypted data.
  def hashAndDecryptWith(is: InputStream, os: OutputStream, cipher: Cipher): Array[Byte] = {
    val sha256 = MessageDigest.getInstance("SHA-256")

    // Stream decrypt using a buffer instead of loading it all in memory
    // Steps: read encrypted data, hash it, decrypt it, write decrypted data
    val buffer = new Array[Byte](8192)
    var count = is.read(buffer)

    while (count != -1) {
      sha256.update(buffer.slice(0, count))
      val decrypted = cipher.update(buffer.slice(0, count))
      os.write(decrypted)
      os.flush()
      count = is.read(buffer)
    }

    val decrypted = cipher.doFinal()
    os.write(decrypted)
    os.flush()

    sha256.digest()
  }

  // Decrypts data from an input stream to an output stream with AES/CBC/PKCS5PADDING
  // Returns a SHA-256 hash of the encrypted data.
  def hashAndDecrypt(is: InputStream, os: OutputStream, initVector: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val iv = new IvParameterSpec(initVector)
    val skeySpec = new SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)

    hashAndDecryptWith(is, os, cipher)
  }
}
