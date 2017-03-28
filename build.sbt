
name := "connect-util"

version := "1.0"

scalaVersion := "2.11.8"

val commonSettings = Seq(
  organization := "nl.focuscura",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-feature", "-explaintypes", "-deprecation"),
  autoCompilerPlugins := true,
  releaseIgnoreUntrackedFiles := true
)

lazy val `connect-http-client` = project
  .settings(commonSettings)
  .settings(
    Dependencies.ws,
    Dependencies.jackson
  )


lazy val root = project.in(file("."))
  .aggregate(`connect-http-client`)
  .settings(commonSettings)


publishTo := {
  val nexus = "https://nexus.focuscura.nl/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots")
  else
    Some("releases"  at nexus + "maven-releases")
}



