package handles.rabbit.messages

case class Delivery[A](body: A, envelope: Envelope, properties: BasicProperties, consumerTag: String) {
  def toResponse[B](response: B): Response[B] = {
    Response(
      payload   = response,
      replyTo   = properties.replyTo.getOrElse(""),
      messageId = properties.messageId.getOrElse(""),
      props     = Some(properties)
    )
  }
}
