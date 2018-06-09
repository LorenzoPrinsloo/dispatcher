package handles.rabbit.messages

import com.rabbitmq.client.BuiltinExchangeType

case class ExchangeConfig(name: String = "amq.topic",
                          `type`: BuiltinExchangeType = BuiltinExchangeType.TOPIC,
                          durable: Boolean = true,
                          autoDelete: Boolean = false,
                          internal: Boolean = false)
