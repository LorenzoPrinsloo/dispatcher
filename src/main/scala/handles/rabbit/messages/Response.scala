package handles.rabbit.messages

case class Response[A](payload: A,
                       replyTo: String,
                       messageId: String,
                       props: Option[BasicProperties] = None)
