maintainer := "chunkwong.wong@gmail.com"
name := "clinet"
organization := "com.github.shinkou"
scalaVersion := "2.13.12"
version := "0.1.0"
mainClass := Some("Main")

libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
    "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
)

enablePlugins(JavaAppPackaging)