package handles.rabbit

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import handles.Logging
import handles.rabbit.messages.{BasicProperties, ExchangeConfig, Message, QueueConfig}
import monix.eval.Task
import monix.execution.Scheduler

import scala.collection.concurrent
import scala.concurrent.Promise
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

class RabbitPublisher(connector: RabbitConnector) {

  /**
    * We use a ConcurrentHashMap to keep track of requests by generating a unique UUID which is used as the correlationId in
    * the RabbitMQ BasicProperties object and the key in the Map. This id is then returned in the response which allows us to complete
    * any Futures waiting on the Promises with the Response Message as an Array of Bytes.
    */
  private val cache: concurrent.Map[UUID, Promise[Array[Byte]]] = {
    new ConcurrentHashMap[UUID, Promise[Array[Byte]]]().asScala
  }

  private lazy val channel: Task[AsyncBrokerChannel] = connector.openAsyncBrokenChannel.memoizeOnSuccess

  private val replyTo = s"rpc-${UUID.randomUUID().toString}"

  private lazy val observable: Task[Unit] = {
    val obs = new RabbitObservable(connector, QueueConfig(replyTo, durable = false, autoDelete = true, exclusive = true), ExchangeConfig())
    obs.onErrorRestartUnlimited.foreachL { delivery =>
      for {
        messageId <- delivery.properties.messageId.map(UUID.fromString)
        promise <- cache.remove(messageId)
      } yield promise.success(delivery.body)
    }.onErrorRestartIf {
      case NonFatal(_) => true
      case _ => false
    }
  }

  def askBytes(message: Message): Task[Array[Byte]] = {
    (for {
      messageId  <- Task.eval(UUID.randomUUID())
      properties <- toBasicProperties(messageId, message, message.options)
      promise    <- submitToCache(messageId)
      _          <- Logging.Info.outgoingL(message)
      _          <- channel.flatMap(_.publish(message.copy(options = Some(properties)).encode))
    } yield promise).flatten
  }

  private def toBasicProperties(messageId: UUID, message: Message, props: Option[BasicProperties]): Task[BasicProperties] = Task.eval {
    def toHeaders(message: Message): Map[String, String] = Map("dst" -> message.routing.src)

    props.getOrElse(BasicProperties()).copy(
      messageId = Some(messageId.toString),
      replyTo = Some(replyTo),
      headers = Some(toHeaders(message))
    )
  }

  private def submitToCache(messageId: UUID): Task[Task[Array[Byte]]] = Task.eval {
    val promise = Promise[Array[Byte]]
    Task.fromFuture(cache.putIfAbsent(messageId, promise).fold(promise)(identity).future)
  }

  def connect(implicit scheduler: Scheduler): Unit = {
    Task.zip2(channel, observable).runAsync
  }
}
