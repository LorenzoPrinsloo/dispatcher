name := "dispatcher"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

libraryDependencies ++= {
  Seq(
    "io.monix" %% "monix" % "2.3.3",
    "ch.lightshed" %% "courier" % "0.1.4",
    "org.typelevel" %% "cats-core" % "1.1.0"
  )
}
