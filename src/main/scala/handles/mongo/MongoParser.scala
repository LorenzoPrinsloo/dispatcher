package handles.mongo

import org.json4s.Formats
import org.json4s.jackson.Serialization.{read, write}
import org.mongodb.scala.bson.collection.immutable.Document
import scala.annotation.implicitNotFound

object MongoParser {

  @implicitNotFound("Unable to find implicit Typeclass instance for DocumentProcessor[${A}, ${B}]")
  trait DocumentParser[A, B] {
    def toDocument(a: A): B

    def fromDocument(b: B): A
  }

  def create[A <: AnyRef: Manifest](implicit formats: Formats): DocumentParser[A, Document] = new DocumentParser[A, Document] {
    def toDocument(a: A): Document = Document(write(a))
    def fromDocument(c: Document): A = read[A](c.toJson)
  }
}

