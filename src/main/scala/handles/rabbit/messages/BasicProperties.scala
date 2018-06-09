package handles.rabbit.messages

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

case class BasicProperties(tpe: Option[String] = None,
                           appId: Option[String] = None,
                           priority: Option[Int] = None,
                           userId: Option[String] = None,
                           replyTo: Option[String] = None,
                           messageId: Option[String] = None,
                           deliveryMode: Option[Int] = None,
                           timestamp: Option[Instant] = None,
                           contentType: Option[String] = None,
                           correlationId: Option[String] = None,
                           contentEncoding: Option[String] = None,
                           expiration: Option[FiniteDuration] = None,
                           headers: Option[Map[String, AnyRef]] = None)
