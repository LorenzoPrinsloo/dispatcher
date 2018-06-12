package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import monix.eval.Task
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import handles.Monix.io
import interfaces.ReportMail
import services.ReportMailer

import scala.util.{Failure, Success}

object Http {

  implicit val formats = Serialization.formats(NoTypeHints)

  val route: Route =
    path("report") {
      post {
        entity(as[ReportMail]) { request =>
          onComplete(ReportMailer.sendReport(request).runAsync) {
            case Success(res) => complete(write(res))
            case Failure(err) => complete(err.getMessage)
          }
        }
      }
    }

}
