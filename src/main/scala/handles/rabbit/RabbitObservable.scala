package handles.rabbit

import handles.rabbit.messages.{Delivery, ExchangeConfig, QueueConfig}
import monix.eval.Callback
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber

class RabbitObservable (connector: RabbitConnector, queue: QueueConfig, exchange: ExchangeConfig) extends Observable[Delivery[Array[Byte]]]{

  private val scheduler = Scheduler.fixedPool("amqp-io", 4)

  override def unsafeSubscribeFn(out: Subscriber[Delivery[Array[Byte]]]): Cancelable = {

    // Callback to pass to runAsync, handles calling onError and onComplete
    val callback = new Callback[Unit] {
      override def onError(ex: Throwable): Unit = out.onError(ex)
      override def onSuccess(value: Unit): Unit = out.onComplete()
    }

    // Open a Channel and begin streaming.
    (for {
      channel <- connector.openAsyncBrokenChannel
      _       <- RabbitConsumer.create(out, channel, queue, exchange)
    } yield ()).runAsync(callback)(scheduler)
  }
}
