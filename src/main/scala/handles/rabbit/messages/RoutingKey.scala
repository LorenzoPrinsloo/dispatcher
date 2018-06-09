package handles.rabbit.messages

case class RoutingKey(
                     src: String,
                     dst: String,
                     exchange: String = "amq.topic")
