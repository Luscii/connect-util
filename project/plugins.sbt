logLevel := Level.Warn

// The Typesafe repository
resolvers ++= Seq(
  //  "Typesafe Maven repository" at "https://repo.typesafe.com/typesafe/maven-releases/",
  //  "Typesafe Ivy repository" at "https://repo.typesafe.com/typesafe/ivy-releases/",
  Resolver.typesafeRepo("releases"),
  "Sonatype repository" at "https://oss.sonatype.org/content/repositories/releases/org/scalastyle/"

)

// Necessary for PlayImport
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.3")

// Publish to Nexus
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.4")


