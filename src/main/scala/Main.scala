package main

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink}
import monix.execution.Scheduler.Implicits.global
import org.slf4j.{Logger, LoggerFactory}
import handles.Akka.system
import handles.Akka.materializer
import api.Http.route

import scala.util.{Failure, Success}


object Main extends App {

  implicit val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  def run(): Unit = {
    Http().bind(interface = "0.0.0.0", port = 8192).to(Sink.foreachParallel(8) {
      connection =>
        connection.handleWith(Flow[HttpRequest].mapAsyncUnordered(parallelism = 16)(Route.asyncHandler(route)))
    }).run().onComplete {
      case Success(res) => logger.info(s"${Console.BLUE} Service listening on: " +
        s"${Console.GREEN}${res.localAddress.getAddress}:${res.localAddress.getPort}${Console.RESET}")
      case Failure(res) => logger.info(s"${Console.RED} Failed to Bind ${Console.RESET}")
    }
  }
  run()
}
