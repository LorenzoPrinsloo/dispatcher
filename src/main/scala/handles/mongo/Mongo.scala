package handles.mongo

import handles.mongo.MongoParser.DocumentParser
import main.Main.logger
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.json4s.Formats
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.immutable.Document

class Mongo {

  implicit val formats: Formats = org.json4s.DefaultFormats
//  implicit val im: DocumentParser[Individual, Document] = MongoParser.create[Individual]

  val MongoCodecs = fromRegistries(MongoClient.DEFAULT_CODEC_REGISTRY)
  val mClient = new MongoConnector("mongodb+srv://lorenzo_admin:Spdf1357@icarus-na70c.mongodb.net", MongoCodecs)

//  implicit val IndividualCol: IndividualsRepository.IndividualsCollection = IndividualsRepository.cIndividual
}
