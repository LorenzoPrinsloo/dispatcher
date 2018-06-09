package handles.rabbit.messages

import com.rabbitmq.client

case class Envelope(exchange: String,
                    deliveryTag: Long,
                    redeliver: Boolean,
                    routingKey: String)

object Envelope {
  implicit class EnvelopOps(private val envelope: client.Envelope) {
    def asScala: Envelope = {
      Envelope(
        exchange    = envelope.getExchange,
        redeliver   = envelope.isRedeliver,
        routingKey  = envelope.getRoutingKey,
        deliveryTag = envelope.getDeliveryTag
      )
    }
  }
}
