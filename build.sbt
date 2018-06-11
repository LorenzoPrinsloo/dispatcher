name := "dispatcher"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.0.11"

libraryDependencies ++= {
  Seq(
    "io.monix" %% "monix" % "2.3.3",
    "ch.lightshed" %% "courier" % "0.1.4",
    "org.typelevel" %% "cats-core" % "1.1.0",
    "org.slf4j" % "slf4j-ext" % "1.7.10",
    "org.slf4j" % "slf4j-simple" % "1.7.10",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"  % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "org.json4s" %% "json4s-jackson" % "3.5.3",
    "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.1",
    "io.netty" % "netty-all" % "4.1.24.Final",
    "com.rabbitmq" % "amqp-client" % "5.2.0",
    "com.github.pathikrit"  %% "better-files-akka"  % "3.5.0"
  )
}
