val scala11 = "2.11.12" // up to 2.11.12
val scala12 = "2.12.13" // up to 2.12.13
val scala13 = "2.13.5"  // up to 2.13.5
// scala13 is waiting on ai.lum %% common.

ThisBuild / scalaVersion := "2.12.15"

lazy val processors = (project in file("."))
  .aggregate(main, corenlp, openie)
  .dependsOn(main, corenlp, openie) // so that we can import from the console
  .settings(
    publish / skip := true
  )

lazy val main = project

lazy val corenlp = project
  .dependsOn(main % "compile -> compile; test -> test")

lazy val openie = project
  .dependsOn(main % "compile -> compile; test -> test")
