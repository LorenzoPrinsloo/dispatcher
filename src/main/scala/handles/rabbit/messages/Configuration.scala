package handles.rabbit.messages

case class Configuration(
                       host: String,
                       port: Int,
                       username: String,
                       password: String,
                       virtualHost: String,
                       requestedChannelMax: Int = 0,
                       requestedFrameMax: Int = 0,
                       heartBeat: Int = 60,
                       connectionTimeout: Int = 60000,
                       handshakeTimeout: Int = 10000,
                       shutdownTimeout: Int = 10000,
                       automaticRecovery: Boolean = true,
                       topologyRecovery: Boolean = true,
                       networkRecoveryInterval: Long = 5000,
                       sslProtocol: Option[String] = None
                     )
