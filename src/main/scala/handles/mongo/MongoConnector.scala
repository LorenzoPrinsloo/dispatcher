package handles.mongo

import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.{ClusterDescription, ClusterType}
import com.mongodb.event._
import com.mongodb.selector.WritableServerSelector
import com.mongodb.{ConnectionString, MongoCredential, ReadPreference}
import io.netty.channel.nio.NioEventLoopGroup
import monix.eval.Task
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.connection.{ClusterSettings, NettyStreamFactoryFactory, SslSettings}
import org.mongodb.scala.{MongoClient, MongoDatabase, WriteConcern}
import org.slf4j.Logger

class MongoConnector(address: String, registry: CodecRegistry)(implicit logger: Logger) extends ClusterListener with CommandListener {

  private var configuration: ClusterDescription = _

  // Cluster Settings
  private val clusterSettings = ClusterSettings.builder()
    .addClusterListener(this)
    .serverSelector(new WritableServerSelector())
    .applyConnectionString(new ConnectionString(address))
    .maxWaitQueueSize(1000)
    .build()

  val eventLoopGroup = new NioEventLoopGroup()

  // Client Settings
  private val clientSettings = MongoClientSettings.builder()
    .readPreference(ReadPreference.primary())
    .writeConcern(WriteConcern.MAJORITY)
    .clusterSettings(clusterSettings)
    .addCommandListener(this)
    .retryWrites(true)
    .codecRegistry(registry)
    .streamFactoryFactory(NettyStreamFactoryFactory.builder().eventLoopGroup(eventLoopGroup).build())
    .credential(MongoCredential.createScramSha1Credential("lorenzo_admin", "admin","Spdf1357".toCharArray))
    .sslSettings(SslSettings.builder()
      .enabled(true)
      .invalidHostNameAllowed(true)
      .build())
    .build()

  @PreDestroy
  def shutDownEventLoopGroup() {
    eventLoopGroup.shutdownGracefully()
  }

  private lazy val mClient: Task[MongoClient] =
   Task(MongoClient(clientSettings)).memoizeOnSuccess

  def getDatabase(name: String): Task[MongoDatabase] = {
    mClient.map(_.getDatabase(name))
  }

  override def clusterOpening(event: ClusterOpeningEvent): Unit = {
    logger.info(s"${Console.BLUE}Opening connection to Mongo Cluster: ${Console.GREEN}${event.getClusterId.getValue}${Console.RESET}.")
  }

  override def clusterDescriptionChanged(event: ClusterDescriptionChangedEvent): Unit = {
    if (event.getPreviousDescription.getType == ClusterType.UNKNOWN && event.getNewDescription.getType != ClusterType.UNKNOWN) {
      logger.info(s"${Console.BLUE}Connection opened successfully to Mongo Cluster: ${Console.GREEN}${event.getClusterId.getValue}${Console.BLUE} with type: ${Console.GREEN}${event.getNewDescription.getType}${Console.RESET}.")
    } else {
      configuration = event.getNewDescription
    }
  }

  override def clusterClosed(event: ClusterClosedEvent): Unit = {
    logger.info(s"${Console.GREEN}Closed connection to Mongo Cluster with id: ${Console.CYAN}${event.getClusterId.getValue}${Console.RESET}.")
  }

  override def commandSucceeded(event: CommandSucceededEvent): Unit = {
    if (event.getCommandName != "ping") {
      logger.trace(s"${Console.GREEN}Processing completed successfully in: ${Console.CYAN}${event.getElapsedTime(TimeUnit.MILLISECONDS)}${Console.GREEN} milliseconds.${Console.RESET}")
    }
  }

  override def commandFailed(event: CommandFailedEvent): Unit = {
    logger.error(s"Mongo command failed", event.getThrowable)
  }

  override def commandStarted(event: CommandStartedEvent): Unit = {
    if (event.getCommandName != "ping") {
      logger.trace(s"${Console.GREEN}Processing Query: ${Console.CYAN}${event.getCommand.toJson}${Console.RESET}")
    }
  }

}
