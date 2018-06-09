package handles.rabbit.encoder

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import scala.annotation.tailrec

object GzipEncoder {

  @tailrec
  def read(length: Int, buffer: Array[Byte], in: GZIPInputStream, out: ByteArrayOutputStream): Array[Byte] = {
    if (length > 0) {
      out.write(buffer, 0, length)
      read(in.read(buffer), buffer, in, out)
    } else {
      out.toByteArray
    }
  }

  def decode(bytes: Array[Byte]): Array[Byte] = {
    val buffer = new Array[Byte](1024)
    val out = new ByteArrayOutputStream()

    val in = new GZIPInputStream(new ByteArrayInputStream(bytes))

    val uncompressedBytes = read(in.read(buffer), buffer, in, out)
    in.close()
    out.close()
    uncompressedBytes
  }

  def encode(bytes: Array[Byte]): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val gos = new GZIPOutputStream(bos)
    gos.write(bytes)
    gos.close()
    bos.toByteArray
  }

  def isEncoded(bytes: Array[Byte]): Boolean = {
    (bytes(0) == GZIPInputStream.GZIP_MAGIC) && (bytes(1) == (GZIPInputStream.GZIP_MAGIC >> 8))
  }
}
