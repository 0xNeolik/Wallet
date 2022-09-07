// IMPORTS
import sbt._
import play.sbt.routes.RoutesKeys

// ****************************
// Generic configuration
// ****************************

// Show full stacktraces when testing
//testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oFG")
//testOptions in IntegrationTest += Tests.Argument("-oFG")

enablePlugins(JavaServerAppPackaging)

enablePlugins(DebianPlugin)

enablePlugins(JDebPackaging)

enablePlugins(SystemdPlugin)

debianPackageDependencies in Debian ++= Seq("oracle-java8-installer")

javaOptions in Test += "-Dconfig.resource=tests.conf"

javaOptions in Universal ++= Seq(
  // JVM memory tuning
  "-J-Xmx512m",
  "-J-Xms512m",
  // Since play uses separate pidfile we have to provide it with a proper path
  // name of the pid file must be play.pid
//  s"-Dpidfile.path=/var/run/${packageName.value}/play.pid",

  // Profiling options
//  "-Dcom.sun.management.jmxremote",
//  "-Dcom.sun.management.jmxremote.port=9010",
//  "-Dcom.sun.management.jmxremote.rmi.port=8000",
//  "-Dcom.sun.management.jmxremote.local.only=false",
//  "-Dcom.sun.management.jmxremote.authenticate=false",
//  "-Dcom.sun.management.jmxremote.ssl=false",

  // alternative, you can remove the PID file
  "-Dpidfile.path=/dev/null"

  // Use separate configuration file for production environment
//  s"-Dconfig.file=/usr/share/${packageName.value}/conf/production.conf",

  // Use separate logger configuration file for production environment
//  s"-Dlogger.file=/usr/share/${packageName.value}/conf/production-logger.xml",
)

scalafmtOnCompile in ThisBuild := true

// ****************************
// Configuration shared by all modules
// ****************************
lazy val commonSettings = Seq(
  // General
  organization := "com.clluc",
  version := "2.3.0-SNAPSHOT",
  scalaVersion := "2.12.6",
  maintainer := "Víctor Villena <victor.villena@clluc.com>",
  packageSummary := "An API that allows people to send personal assets",
  packageDescription := "An API that allows people to send personal assets",
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint", // Enable recommended additional warnings.
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-language:higherKinds" // Enable the use of higher kinds by default
  ),
  // Keep warnings as warning when using the Scala REPL
  scalacOptions in (Compile, console) ~= (_.filterNot(_ == "-Xfatal-warnings")),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  // Add Jcenter repository (needed for some JWT packages)
  resolvers += Resolver.jcenterRepo
)

val akkaVersion       = "2.5.4"
val circeVersion      = "0.8.0"
val doobieVersion     = "0.4.4"
val silhouetteVersion = "5.0.2"
val monocleVersion    = "1.4.0"

lazy val commonDependencies = Seq(
  // Testing
  ("org.scalatest"  %% "scalatest"  % "3.0.1"  % "test").withSources().withJavadoc(),
  ("org.scalacheck" %% "scalacheck" % "1.13.4" % "test").withSources().withJavadoc(),
  // Logging
  ("com.typesafe.scala-logging" %% "scala-logging"  % "3.5.0").withSources(),
  ("ch.qos.logback"             % "logback-classic" % "1.1.7").withSources(),
  // DI
  guice
)

lazy val circeDependencies = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-shapes",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-optics"
).map(_ % circeVersion)

lazy val jodaTimeDependencies = Seq(
  "joda-time" % "joda-time"    % "2.9.9",
  "org.joda"  % "joda-convert" % "1.8.1"
)

// ****************************
// Core project definition
// ****************************
lazy val coreDependencies = Seq(
  // Joda time
  "joda-time" % "joda-time" % "2.9.9",
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  // Cats
  ("org.typelevel" %% "cats" % "0.9.0").withSources().withJavadoc(),
  // Caching
  "com.google.guava" % "guava" % "23.0",
  // Testing
  "com.typesafe.akka"          %% "akka-testkit"                % akkaVersion    % "test",
  "org.scalamock"              %% "scalamock-scalatest-support" % "3.6.0"        % "test",
  "com.github.julien-truffaut" %% "monocle-core"                % monocleVersion % "test",
  "com.github.julien-truffaut" %% "monocle-macro"               % monocleVersion % "test",
  //CipherUtils
  "commons-net"     % "commons-net"   % "3.6",
  "net.sf.proguard" % "proguard-base" % "5.3",
  //Scheduler
  "com.enragedginger" % "akka-quartz-scheduler_2.12" % "1.7.0-akka-2.5.x"
) ++ circeDependencies ++ jodaTimeDependencies

lazy val core = project
  .settings(
    commonSettings,
    // Dependencies
    libraryDependencies ++= commonDependencies ++ coreDependencies
  )
  .dependsOn(`primary-ports`, model, `secondary-ports`)

// ****************************
// Common model project definition (ubiquitous language)
// ****************************
lazy val model = project
  .settings(
    commonSettings,
    // Dependencies
    libraryDependencies ++= commonDependencies ++ circeDependencies
  )

// ****************************
// Primary ports
// ****************************
lazy val `primary-ports` = project
  .settings(
    commonSettings,
    // Dependencies
    libraryDependencies ++= commonDependencies ++ jodaTimeDependencies
  )
  .dependsOn(model)

// ****************************
// Secondary ports
// ****************************
lazy val secondaryPortsDependencies = jodaTimeDependencies

lazy val `secondary-ports` = project
  .settings(
    commonSettings,
    // Dependencies
    libraryDependencies ++= commonDependencies ++ secondaryPortsDependencies
  )
  .dependsOn(model)

// ****************************
// App entry point definition
// ****************************
lazy val entryPointDependencies = Seq(
  // Play
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  // Silhouette
  "com.mohiva"     %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "com.mohiva"     %% "play-silhouette-crypto-jca"      % silhouetteVersion,
  "net.codingwell" %% "scala-guice"                     % "4.1.0",
  "com.iheart"     %% "ficus"                           % "1.4.1", // Typesafe config extension utility methods
  "com.mohiva"     %% "play-silhouette-testkit"         % silhouetteVersion % "test",
  "org.scalamock"  %% "scalamock-scalatest-support"     % "3.6.0" % "test"
) ++ circeDependencies

lazy val entryPoint = (project in file("."))
  .settings(Defaults.itSettings: _*)
  .settings(
    commonSettings,
    // Project specific settings
    scalaSource in IntegrationTest := baseDirectory.value / "it",
    resourceDirectory in IntegrationTest := baseDirectory.value / "it-conf",
    parallelExecution in IntegrationTest := false,
    routesGenerator := InjectedRoutesGenerator,
    RoutesKeys.generateReverseRouter := false,
    name := "stockmind-api",
    coverageExcludedPackages := "router.*",
    routesImport := Seq.empty, // Avoid an 'unused import' error when not using the Assets controller in the routes file
    // Dependencies
    libraryDependencies ++= commonDependencies ++ entryPointDependencies
  )
  .dependsOn(
    `primary-ports`,
    `secondary-ports`,
    core,
    `postgres-secondary-adapter`,
    `ethereum-secondary-adapter`,
    `twitter-secondary-adapter`,
    `twitter-users-directory-secondary-adapter`,
    controllers
  )
  .enablePlugins(PlayScala)
  .configs(IntegrationTest)
  .aggregate(
    core,
    `postgres-secondary-adapter`,
    `ethereum-secondary-adapter`,
    `twitter-secondary-adapter`,
    `twitter-users-directory-secondary-adapter`,
    model,
    `primary-ports`,
    `secondary-ports`,
    controllers
  )

// ****************************
// Controllers
// ****************************

lazy val controllersDependencies = Seq(
  "com.typesafe.play" %% "play"                        % "2.6.3",
  "com.mohiva"        %% "play-silhouette"             % silhouetteVersion,
  "com.mohiva"        %% "play-silhouette-persistence" % silhouetteVersion,
  "com.mohiva"        %% "play-silhouette-cas"         % silhouetteVersion,
  filters,
  ehcache
) ++ circeDependencies

lazy val controllers = project
  .settings(
    commonSettings,
    libraryDependencies ++= commonDependencies ++ controllersDependencies
  )
  .dependsOn(
    `primary-ports`
  )

// ****************************
// Secondary adapter (Postgresql) definition
// ****************************
lazy val postgresSecondaryAdapterDependencies = Seq(
  // Database communication
  "org.tpolecat" %% "doobie-core-cats"     % doobieVersion,
  "org.tpolecat" %% "doobie-postgres-cats" % doobieVersion,
  // Integration tests
  ("org.scalatest"     %% "scalatest"           % "3.0.1"  % "test,it").withSources().withJavadoc(),
  ("org.scalacheck"    %% "scalacheck"          % "1.13.4" % "test,it").withSources().withJavadoc(),
  ("com.fortysevendeg" %% "scalacheck-datetime" % "0.2.0"  % "test,it").withSources().withJavadoc(),
  ("com.typesafe"      % "config"               % "1.3.1"  % "test,it").withSources().withJavadoc()
)

lazy val `postgres-secondary-adapter` = project
  .settings(Defaults.itSettings: _*)
  .settings(
    commonSettings,
    // Project specific settings
    unmanagedSourceDirectories in Test += baseDirectory.value / "src" / "testdeps" / "scala",
    unmanagedSourceDirectories in IntegrationTest += baseDirectory.value / "src" / "testdeps" / "scala",
    parallelExecution in IntegrationTest := false,
    testOptions in IntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oFG"),
    // Dependencies
    libraryDependencies ++= commonDependencies ++ postgresSecondaryAdapterDependencies
  )
  .dependsOn(`secondary-ports`)
  .configs(IntegrationTest)

// ****************************
// Secondary adapter (Ethereum API client) definition
// ****************************
lazy val ethereumSecondaryAdapterDependencies = Seq(
  ws,
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  // Shapeless
  "com.chuusai" %% "shapeless" % "2.3.2",
  // Integration tests
  ("org.scalatest"  %% "scalatest"  % "3.0.1"  % "it").withSources().withJavadoc(),
  ("org.scalacheck" %% "scalacheck" % "1.13.4" % "it").withSources().withJavadoc()
) ++ circeDependencies

lazy val `ethereum-secondary-adapter` = project
  .settings(Defaults.itSettings: _*)
  .settings(
    commonSettings,
    // Project specific settings
    parallelExecution in IntegrationTest := false,
    // Dependencies
    libraryDependencies ++= commonDependencies ++ ethereumSecondaryAdapterDependencies
  )
  .dependsOn(`secondary-ports`)
  .configs(IntegrationTest)

// ****************************
// Secondary adapter (Twitter API client) definition
// ****************************
lazy val twitterSecondaryAdapterDependencies = Seq(
  // Twitter lib
  "org.twitter4j" % "twitter4j-core" % "4.0.4"
)

lazy val `twitter-secondary-adapter` = project
  .settings(
    commonSettings,
    libraryDependencies ++= commonDependencies ++ twitterSecondaryAdapterDependencies
  )
  .dependsOn(`secondary-ports`)

// ****************************
// Secondary adapter (Twitter based users directory) definition
// ****************************
lazy val twitterUsersDirectorySecondaryAdapterDependencies = Seq(
  // Twitter lib
  ("org.twitter4j" % "twitter4j-core" % "4.0.4").withSources().withJavadoc(),
  // Cats
  ("org.typelevel" %% "cats" % "0.9.0").withSources().withJavadoc()
)

lazy val `twitter-users-directory-secondary-adapter` = project
  .settings(
    commonSettings,
    libraryDependencies ++= commonDependencies ++ twitterUsersDirectorySecondaryAdapterDependencies
  )
  .dependsOn(`secondary-ports`)
