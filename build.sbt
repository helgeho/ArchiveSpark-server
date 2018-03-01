lazy val commonSettings = Seq(
  name := "archivespark-server",
  version := "1.0",
  scalaVersion := "2.11.12"
)

lazy val web2warc = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "2.2.0" excludeAll(
        ExclusionRule(organization = "org.apache.httpcomponents", name = "httpclient"),
        ExclusionRule(organization = "org.apache.httpcomponents", name = "httpcore")),
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.scalatra" %% "scalatra" % "2.5.4",
      "org.scalatra" %% "scalatra-cache-guava" % "2.5.4",
      "org.scalatra" %% "scalatra-scalatest" % "2.5.4" % "test",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
      "org.eclipse.jetty" % "jetty-webapp" % "9.2.19.v20160908" % "container;compile",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
      "com.github.helgeho" %% "archivespark" % "2.7.6" % "provided"
    )
  )

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, cacheOutput = false)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

enablePlugins(ScalatraPlugin)