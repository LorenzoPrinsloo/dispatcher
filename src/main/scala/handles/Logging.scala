package handles

import java.nio.charset.StandardCharsets

import handles.rabbit.messages.{Delivery, Message, RoutingKey}
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}

object Logging {

  val logger: Logger = LoggerFactory.getLogger("rabbit")

  object Info {

    def outgoingL(message: Message): Task[Unit] = {
      for {
        _ <- Task.eval(oTrace(message))
        _ <- Task.eval(oDebug(message))
      } yield ()
    }

    private def iDebug(delivery: => Delivery[Array[Byte]]): Unit =  {
      logger.debug(
        s"${Console.BLUE}event:${Console.GREEN} delivery${Console.MAGENTA} - " +
          s"${Console.BLUE}status:${Console.GREEN} ${delivery.properties.headers.flatMap(_.get("status")).getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}correlationid:${Console.GREEN} ${delivery.properties.correlationId.getOrElse("none")}${Console.MAGENTA} - " +
          s"${Console.BLUE}src:${Console.GREEN} ${delivery.properties.headers.flatMap(_.get("src")).getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}routingkey:${Console.GREEN} ${delivery.envelope.routingKey}${Console.MAGENTA} - " +
          s"${Console.BLUE}exchange:${Console.GREEN} ${ if (delivery.envelope.exchange == "") "amq.default" else delivery.envelope.exchange}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-type:${Console.GREEN} ${delivery.properties.contentType.getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-encoding:${Console.GREEN} ${delivery.properties.contentEncoding.getOrElse("identity")}${Console.RESET}"
      )
    }

    private def iTrace(delivery: => Delivery[Array[Byte]], bytes: => Array[Byte]): Unit = {
      logger.trace(
        s"${Console.BLUE}event:${Console.GREEN} delivery${Console.MAGENTA} - " +
          s"${Console.BLUE}status:${Console.GREEN} ${delivery.properties.headers.flatMap(_.get("status")).getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}correlationid:${Console.GREEN} ${delivery.properties.correlationId.getOrElse("none")}${Console.MAGENTA} - " +
          s"${Console.BLUE}src:${Console.GREEN} ${delivery.properties.headers.flatMap(_.get("src")).getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}routingkey:${Console.GREEN} ${delivery.envelope.routingKey}${Console.MAGENTA} - " +
          s"${Console.BLUE}exchange:${Console.GREEN} ${ if (delivery.envelope.exchange == "") "amq.default" else delivery.envelope.exchange}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-type:${Console.GREEN} ${delivery.properties.contentType.getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-encoding:${Console.GREEN} ${delivery.properties.contentEncoding.getOrElse("identity")}${Console.MAGENTA} - " +
          s"${Console.BLUE}body:${Console.GREEN} ${new String(bytes, StandardCharsets.UTF_8)}${Console.RESET}"
      )
    }

    private def oDebug(message: Message): Unit = {
      logger.debug(
        s"${Console.BLUE}event:${Console.GREEN} publish${Console.MAGENTA} - " +
          s"${Console.BLUE}status:${Console.GREEN} pending${Console.MAGENTA} - " +
          s"${Console.BLUE}src:${Console.GREEN} ${message.routing.src}${Console.MAGENTA} - " +
          s"${Console.BLUE}routingkey:${Console.GREEN} ${message.routing.dst}${Console.MAGENTA} - " +
          s"${Console.BLUE}exchange:${Console.GREEN} ${ if (message.routing.exchange == "") "amq.default" else message.routing.exchange}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-type:${Console.GREEN} ${message.options.flatMap(_.contentType).getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-encoding:${Console.GREEN} ${message.options.flatMap(_.contentEncoding).getOrElse("identity")}${Console.MAGENTA} - "
      )
    }

    private def oTrace(message: Message): Unit = {
      logger.trace(
        s"${Console.BLUE}event:${Console.GREEN} publish${Console.MAGENTA} - " +
          s"${Console.BLUE}status:${Console.GREEN} pending${Console.MAGENTA} - " +
          s"${Console.BLUE}src:${Console.GREEN} ${message.routing.src}${Console.MAGENTA} - " +
          s"${Console.BLUE}routingkey:${Console.GREEN} ${message.routing.dst}${Console.MAGENTA} - " +
          s"${Console.BLUE}exchange:${Console.GREEN} ${ if (message.routing.exchange == "") "amq.default" else message.routing.exchange}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-type:${Console.GREEN} ${message.options.flatMap(_.contentType).getOrElse("unknown")}${Console.MAGENTA} - " +
          s"${Console.BLUE}content-encoding:${Console.GREEN} ${message.options.flatMap(_.contentEncoding).getOrElse("identity")}${Console.MAGENTA} - " +
          s"${Console.BLUE}body:${Console.GREEN} ${new String(message.bytes, StandardCharsets.UTF_8)}${Console.RESET}"
      )
    }

    def incoming(delivery: => Delivery[Array[Byte]], bytes: => Array[Byte]): Unit = {
      iTrace(delivery, bytes)
      iDebug(delivery)
    }

    def queueBind(exchange: String, queue: String, routingKey: RoutingKey): Unit = {
      logger.info(
        s"${Console.BLUE}Creating Binding between Exchange: ${Console.GREEN}$exchange${Console.BLUE} and Queue: " +
          s"${Console.GREEN}$queue${Console.BLUE} with RoutingKey: ${Console.GREEN}${routingKey.dst}${Console.RESET}"
      )
    }

    def queueBindAsync(exchange: String, queue: String, routingKey: RoutingKey): Task[Unit] = Task.eval {
      logger.info(
        s"${Console.BLUE}Creating Binding between Exchange: ${Console.GREEN}$exchange${Console.BLUE} and Queue: " +
          s"${Console.GREEN}$queue${Console.BLUE} with RoutingKey: ${Console.GREEN}${routingKey.dst}${Console.RESET}"
      )
    }
  }

  object Error {
    def connectionBind(host: String, port: Int, message: String, attempts: Long, backoffFactor: Long): Task[Unit] = Task.eval {
      logger.error(
        s"${Console.RED}Error connecting to $host:$port - $message." +
          s" About to retry connection in ${attempts * backoffFactor} seconds.${Console.RESET}"
      )
    }
  }
}
