package filesharer
package encryptor

import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

object Encryptor {
  val key = "0123456789ABCDEF".getBytes("UTF-8")
  val initVector = "0123456789ABCDEF".getBytes("UTF-8")

  def encryptTo(is: InputStream, os: OutputStream): Unit = {
    val encrypted = encrypt(is.readAllBytes())
    os.write(encrypted)
    os.flush()
  }

  def encryptFileTo(from: String, to: String): Unit = {
    cryptFileTo(encryptFile, from, to)
  }

  def decryptFileTo(from: String, to: String): Unit = {
    cryptFileTo(decryptFile, from, to)
  }

  def cryptFileTo(f: (FileInputStream, FileOutputStream) => Unit, from: String, to: String): Unit = {
    val src = new File(from)
    val dst = new File(to)
    dst.createNewFile()

    val srcs = new FileInputStream(src)
    val dsts = new FileOutputStream(dst)

    f(srcs, dsts)
  }

  def encryptFile(in: FileInputStream, out: FileOutputStream): Unit = {
    val encrypted = encrypt(in.readAllBytes())
    out.write(encrypted)
  }

  def encrypt(bytes: Array[Byte]): Array[Byte] = {
    val iv = new IvParameterSpec(initVector)
    val skeySpec = new SecretKeySpec(key, "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)

    cipher.doFinal(bytes)
  }

  def decryptFile(in: FileInputStream, out: FileOutputStream): Unit = {
    val decrypted = decrypt(in.readAllBytes())
    out.write(decrypted)
  }

  def decrypt(bytes: Array[Byte]): Array[Byte] = {
    val iv = new IvParameterSpec(initVector)
    val skeySpec = new SecretKeySpec(key, "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)
    cipher.doFinal(bytes)
  }
}