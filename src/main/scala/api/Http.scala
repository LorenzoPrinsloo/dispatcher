package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import monix.eval.Task
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import handles.Monix.io

import scala.util.{Failure, Success}

object Http {

  implicit val formats = Serialization.formats(NoTypeHints)

  val route: Route =
    path("individual") {
      get {
        onComplete(Task.eval{}.runAsync) {
//          case Success(res) => complete(write(res))
          case Failure(err) => complete(err.getMessage)
        }
      }
    }

}
