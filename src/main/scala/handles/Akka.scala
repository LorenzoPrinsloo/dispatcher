package handles

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object Akka {
  implicit val system: ActorSystem = ActorSystem.create("ActorSystem", ConfigFactory.load("application.conf"))
  implicit val materializer: ActorMaterializer = ActorMaterializer()
}
