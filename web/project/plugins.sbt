// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.6")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.14")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")