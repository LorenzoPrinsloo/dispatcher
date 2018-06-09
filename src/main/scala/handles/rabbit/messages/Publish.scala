package handles.rabbit.messages

case class Publish[A](payload: A,
                      correlationId: String,
                      routing: RoutingKey,
                      props: Option[Configuration] = None)
