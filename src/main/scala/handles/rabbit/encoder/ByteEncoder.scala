package handles.rabbit.encoder

import monix.eval.Task

trait ByteEncoder {

  def encode(bytes: Array[Byte]): Array[Byte] = GzipEncoder.encode(bytes)

  def encodeAsync(bytes: Array[Byte]): Task[Array[Byte]] = Task(encode(bytes))

  def decode(bytes: Array[Byte]): Array[Byte] = GzipEncoder.decode(bytes)

  def decodeAsync(bytes: Array[Byte]): Task[Array[Byte]] = Task(decode(bytes))
}

object ByteEncoder extends ByteEncoder
