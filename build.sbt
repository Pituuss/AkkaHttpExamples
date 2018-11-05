name := "AkkaHTTP"

version := "0.1"

scalaVersion := "2.12.7"
lazy val akkaVersion = "2.5.16"
lazy val akkaHttpVersion = "10.1.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion