package handles.rabbit

import java.io.IOException
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.rabbitmq.client
import com.rabbitmq.client.{AMQP, DefaultConsumer, ShutdownSignalException}
import handles.Logging
import handles.rabbit.encoder.ByteEncoder
import handles.rabbit.messages._
import monix.eval.Task
import monix.execution.Ack.{Continue, Stop}
import monix.execution.atomic.Atomic
import monix.reactive.observers.Subscriber
import handles.rabbit.implicits.TaskImplicits._
import handles.Logging._

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

class RabbitConsumer(out: Subscriber[Delivery[Array[Byte]]], channel: AsyncBrokerChannel, queue: QueueConfig, exchange: ExchangeConfig) extends DefaultConsumer(channel.channel) {

  // This boolean is used to tell whether or not an error has been thrown
  private val isDone: Atomic[Boolean] = Atomic(false)
  private val isError: Atomic[Boolean] = Atomic(false)

  // Don't copy this style
  private var throwable: Throwable = _

  // RabbitMQ Message Buffer - used for storing deliveries received asynchronously from the RabbitMQ broker.
  private val buffer = new ArrayBlockingQueue[Delivery[Array[Byte]]](queue.prefetch)

  /**
    * Initializes the consumer and begins receiving messages from the RabbitMQ
    * broker. This also commences the run-loop that streams messages to any subscribers.
    *
    * @return A task of unit.
    */
  def subscribe(): Task[Unit] = {
    (for {
      _ <- channel.queueDeclare(queue)
      _ <- channel.exchangeDeclare(exchange)
      _ <- channel.basicQos(queue.prefetch)
      _ <- channel.basicConsume(queue.name, queue.autoAck, this)
      _ <- runLoop()
    } yield ()).onErrorHandleWithLogging(cancelSubscriptionOnError)(Logging.logger)
  }

  /**
    * This is effectively the run-loop that feeds the subscriber, Monix Task prevent this
    * recursive function from blowing the stack.
    *
    * @return Unit
    */
  def runLoop(): Task[Unit] = {
    if (isDone.get) {
      Task(cancelSubscriptionOnComplete())
    } else if (isError.get) {
      Task(cancelSubscriptionOnError(throwable))
    } else {
      Task(buffer.poll(5, TimeUnit.MILLISECONDS)).flatMap { elem =>
        if (elem == null) runLoop() else {
          Task.fromFuture(out.onNext(elem)).flatMap {
            case Stop =>
              channel.basicNack(elem.envelope.deliveryTag, multiple = false, requeue = true).map { _ =>
                cancelSubscriptionOnComplete()
              }
            case Continue =>
              if (!queue.autoAck) {
                channel.basicAck(elem.envelope.deliveryTag, multiple = true).flatMap { _ =>
                  runLoop()
                }.onErrorHandle(cancelSubscriptionOnError)
              } else {
                runLoop()
              }
          }
        }
      }
    }
  }

  /**
    * Asynchronously handle messages routed to this queue. Converts the delivery to a more idiomatic scala object,
    * and then either acknowledges the delivery if the queue is set to autoAck, it then adds the delivery to the
    * message buffer.
    *
    * @param consumerTag The consumer tag assigned to this consumer when it is registered with the RabbitMQ broker.
    * @param envelope    The deliveries basic routing properties.
    * @param bp  The deliveries properties, these differ from the envelope in that they do not include routing information.
    * @param body        The message body in bytes
    */
  override def handleDelivery(consumerTag: String, envelope: client.Envelope, bp: AMQP.BasicProperties, body: Array[Byte]): Unit = try {
    if (isDone.get || isError.get) {
      channel.unsafeBasicNack(envelope.getDeliveryTag, multiple = false, requeue = true)
    } else {
      if (bp.getContentEncoding == null || bp.getContentType == null) {
        channel.unsafeBasicNack(envelope.getDeliveryTag, multiple = false, requeue = false)
      } else {
        val unpacked = ByteEncoder.decode(body)
        val env = Envelope(
          exchange    = envelope.getExchange,
          redeliver   = envelope.isRedeliver,
          routingKey  = envelope.getRoutingKey,
          deliveryTag = envelope.getDeliveryTag
        )

        val prop = BasicProperties(
          tpe             = Option(bp.getType),
          appId           = Option(bp.getAppId),
          userId          = Option(bp.getUserId),
          replyTo         = Option(bp.getReplyTo),
          priority        = Option(bp.getPriority),
          timestamp       = Option(bp.getTimestamp.toInstant),
          messageId       = Option(bp.getMessageId),
          expiration      = Option(bp.getExpiration).map(ttl => FiniteDuration.apply(ttl.toLong, TimeUnit.MILLISECONDS)),
          contentType     = Option(bp.getContentType),
          deliveryMode    = Option(bp.getDeliveryMode),
          correlationId   = Option(bp.getCorrelationId),
          contentEncoding = Option(bp.getContentEncoding)
        )

        val delivery = Delivery(unpacked, env, prop, consumerTag)
        buffer.add(delivery)
        if (queue.autoAck) {
          Logging.Info.incoming(delivery, unpacked)
          channel.unsafeBasicAck(delivery.envelope.deliveryTag, multiple = false)
        } else {
          Logging.Info.incoming(delivery, unpacked)
        }
      }
    }
  } catch {
    case NonFatal(_: IOException) => channel.unsafeBasicNack(envelope.getDeliveryTag, multiple = false, requeue = false)
    case NonFatal(ex) => cancelSubscriptionOnError(ex)
  }

  /**
    * This callback is fired when the consumer is successfully registered with the RabbitMQ broker.
    *
    * @param consumerTag The successfully registered consumerTag
    */
  override def handleConsumeOk(consumerTag: String): Unit = {
    logger.info(s"${Console.BLUE}Consumer for Queue: ${Console.GREEN}${queue.name}${Console.BLUE} successfully registered.${Console.RESET}")
  }

  /**
    * Handles shutdown signals from rabbit. If it is a channel level exception we restart the stream which
    * tries to recreate a channel. If it is initiated by the application we call onComplete.
    *
    * @param consumerTag The id of the consumer that the exception is destined for.
    * @param sig         The shutdown signal exception
    */
  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit = {
    if (sig.isInitiatedByApplication && !isError.get) {
      isDone.set(true)
    } else if (AsyncBrokerChannel.isChannelLevelException(sig) && !isDone.get) {
      isError.set(true)
      throwable = sig
    } else {
      // Connection errors should be recoverable by the rabbit java driver.
    }
  }

  /**
    * Cancels this consumer and then closes the channel in the event of any errors or if a Stop is
    * received from downstream. This allows the observable to try and reconnect, re-establishing the channel
    * and consumer if an error strategy has been defined.
    *
    * @return A task of unit.
    */
  private def cancelSubscriptionOnComplete(): Unit = try {
    channel.unsafeClose()
    out.onComplete()
  } catch {
    case NonFatal(ex) => out.onError(ex)
  }

  /**
    * Cancel a subscription with a specific throwable instance.
    *
    * @param t The throwable that should result in the cancellation
    */
  private def cancelSubscriptionOnError(t: Throwable): Unit = try {
    channel.unsafeClose()
    out.onError(t)
  } catch {
    case NonFatal(ex) => out.onError(ex)
  }
}

object RabbitConsumer {
  def create(out: Subscriber[Delivery[Array[Byte]]], channel: AsyncBrokerChannel, queue: QueueConfig, exchange: ExchangeConfig): Task[Unit] = {
    new RabbitConsumer(out, channel, queue, exchange).subscribe()
  }
}
