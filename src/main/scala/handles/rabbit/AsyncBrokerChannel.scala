package handles.rabbit

import cats.data.Kleisli
import com.rabbitmq.client.AMQP.Queue.DeclareOk
import com.rabbitmq.client.AMQP.{BasicProperties, Exchange, Queue}
import com.rabbitmq.client._
import handles.Logging
import monix.eval.Task
import handles.rabbit.implicits.TaskImplicits._
import handles.rabbit.messages.{ExchangeConfig, Message, QueueConfig, RoutingKey}

class AsyncBrokerChannel(connection: Connection) {

  var channel = connection.createChannel()

  private def synchronize[A](command: Channel => A): Task[A] =
    channel.synchronized(
      Task.eval(command(channel)).onErrorRetryIfSync(AsyncBrokerChannel.isChannelLevelException) {
        connection.createChannel()
      }
    )

  def publish(message: Message): Task[Unit] = {
    synchronize(_.basicPublish(message.routing.exchange, message.routing.dst, new BasicProperties(), message.bytes))
  }

  def exchangeDeclare(conf: ExchangeConfig): Task[Exchange.DeclareOk] = {
    synchronize(_.exchangeDeclare(conf.name, conf.`type`, conf.durable, conf.autoDelete, conf.internal, null))
  }

  def basicQos(prefetch: Int): Task[Unit] = {
    synchronize(_.basicQos(prefetch, false))
  }

  def queueDeclare(): Task[DeclareOk] = {
    synchronize(_.queueDeclare())
  }

  def queueDeclare(conf: QueueConfig): Task[DeclareOk] = {
    synchronize(_.queueDeclare(conf.name, conf.durable, conf.exclusive, conf.autoDelete, null))
  }

  def queueBind(queue: String, exchange: String, routingKey: RoutingKey): Task[Queue.BindOk] = {
    synchronize(_.queueBind(queue, exchange, routingKey.dst, null))
  }

  def bindRoutingKey(routingKey: RoutingKey): Kleisli[Task, (QueueConfig, ExchangeConfig), Unit] = Kleisli { case (queue, exchange) =>
    for {
      _ <- exchangeDeclare(exchange)
      _ <- queueDeclare(queue)
      _ <- synchronize(_.queueBind(queue.name, exchange.name, routingKey.dst, null))
    } yield Logging.Info.queueBind(exchange.name, queue.name, routingKey)
  }

  def basicConsume(queue: String, autoAck: Boolean, consumer: Consumer): Task[String] = {
    synchronize(_.basicConsume(queue, autoAck, consumer))
  }

  def basicAck(deliveryTag: Long, multiple: Boolean): Task[Unit] = {
    synchronize(_.basicAck(deliveryTag, multiple))
  }

  def basicNack(deliveryTag: Long, multiple: Boolean, requeue: Boolean): Task[Unit] = {
    synchronize(_.basicNack(deliveryTag, multiple, requeue))
  }

  def unsafeBasicAck(deliveryTag: Long, multiple: Boolean): Unit = {
    channel.basicAck(deliveryTag, multiple)
  }

  def unsafeBasicNack(deliveryTag: Long, multiple: Boolean, requeue: Boolean): Unit = {
    channel.basicNack(deliveryTag, multiple, requeue)
  }

  def unsafeClose(): Unit = {
    channel.close()
  }

}

object AsyncBrokerChannel {

  def isChannelLevelException(cause: Throwable): Boolean = cause match {
    case c: ShutdownSignalException =>
      c.getReason match {
        case close: AMQP.Channel.Close =>
          close.getReplyCode match {
            case 200 => true
            case 311 => true
            case 313 => true
            case 403 => true
            case 404 => true
            case 405 => true
            case 406 => true
            case _ => false
          }
        case _ => false
      }
    case _ => false
  }
}