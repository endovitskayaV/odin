lazy val commonSettings = Seq(
  organization := "org.clulab",
  scalaVersion := "2.13.8",
  scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")
)

lazy val processors = (project in file("."))
  .settings(commonSettings)
  .aggregate(main, odin, corenlp, openie)
  .dependsOn(main, odin, corenlp, openie) // so that we can import from the console

lazy val main = project
  .settings(commonSettings)

lazy val odin = project
  .settings(commonSettings)
  .dependsOn(main % "test->test;compile->compile")

lazy val corenlp = project
  .settings(commonSettings)
  .dependsOn(main % "test->test;compile->compile")

lazy val openie = project
  .settings(commonSettings)
  .dependsOn(main % "test->test;compile->compile", odin)