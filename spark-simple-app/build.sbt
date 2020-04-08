name := "spark-simple-app"
version := "0.1"
scalaVersion :="2.10.4"
libraryDependencies ++=
Seq("org.apache.spark" % "spark-core_2.10" % "1.6.1" % "provided", "joda-time" % "joda-time" % "2.8.2")
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

