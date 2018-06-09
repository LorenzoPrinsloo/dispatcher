package handles.rabbit.messages

case class QueueConfig(name: String,
                       prefetch: Int = 100,
                       autoAck: Boolean = false,
                       durable: Boolean = true,
                       exclusive: Boolean = false,
                       autoDelete: Boolean = false)
