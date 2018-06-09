package handles.rabbit

import java.util.concurrent.Executors

import com.rabbitmq.client.{Connection, ConnectionFactory}
import handles.Logging
import handles.rabbit.messages.Configuration
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}

import scala.util.control.NonFatal

class RabbitConnector private (conf: Configuration) {

  implicit val logger: Logger = Logging.logger
  private val ec = Executors.newFixedThreadPool(10)

  private lazy val configuration: ConnectionFactory = {
    val factory = new ConnectionFactory()
    factory.setHost(conf.host)
    factory.setPort(conf.port)
    factory.setSharedExecutor(ec)
    factory.setShutdownExecutor(ec)
    factory.setUsername(conf.username)
    factory.setPassword(conf.password)
    factory.setVirtualHost(conf.virtualHost)
    factory.setRequestedHeartbeat(conf.heartBeat)
    factory.setAutomaticRecoveryEnabled(conf.automaticRecovery)
    factory.setTopologyRecoveryEnabled(conf.topologyRecovery)
    factory.setNetworkRecoveryInterval(5000)
    conf.sslProtocol.foreach(factory.useSslProtocol)
    factory
  }

  private def connect(attempts: Long, exponentialBackOffFactor: Long): Connection = {
    try {
      configuration.newConnection()
    } catch {
      case NonFatal(ex) =>
        Logging.Error.connectionBind(configuration.getHost, configuration.getPort, ex.getMessage, attempts, exponentialBackOffFactor)
        Thread.sleep(attempts * 1000)
        connect(attempts * exponentialBackOffFactor, exponentialBackOffFactor)
    }
  }

  private lazy val connection: Connection = connect(1 ,2)

  def openAsyncBrokenChannel: Task[AsyncBrokerChannel] = Task.eval {
    new AsyncBrokerChannel(connection)
  }
}

object RabbitConnector {
   def create(conf: Configuration): RabbitConnector = new RabbitConnector(conf)
}
