name := "arxiv"

version := "1.0"

lazy val `arxiv` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  specs2 % Test,
  guice,
  "joda-time" % "joda-time" % "2.9.9"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")
