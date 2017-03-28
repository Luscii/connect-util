import play.sbt.PlayImport
import sbt._
import sbt.Keys._
import sbt.Def.Setting


object Dependencies {

  private def deps(modules: ModuleID*): Setting[Seq[ModuleID]] = libraryDependencies ++= modules

  val jackson = deps(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.2"
  )

  val ws = deps(
    // force netty-reactive-streams transitive dep to 1.0.5
    //    "com.typesafe.netty" % "netty-reactive-streams" % "1.0.5",
    PlayImport.ws.copy(revision = "2.5.3") //exclude("com.typesafe.netty", "netty-reactive-streams")
  )

}

