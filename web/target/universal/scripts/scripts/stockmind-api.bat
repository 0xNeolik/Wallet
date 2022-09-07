@REM stockmind-api launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (optional if java on path)
@REM CFG_OPTS  - JVM options (optional)
@REM Configuration:
@REM STOCKMIND_API_config.txt found in the STOCKMIND_API_HOME.
@setlocal enabledelayedexpansion

@echo off

if "%STOCKMIND_API_HOME%"=="" set "STOCKMIND_API_HOME=%~dp0\\.."

set "APP_LIB_DIR=%STOCKMIND_API_HOME%\lib\"

rem Detect if we were double clicked, although theoretically A user could
rem manually run cmd /c
for %%x in (!cmdcmdline!) do if %%~x==/c set DOUBLECLICKED=1

rem FIRST we load the config file of extra options.
set "CFG_FILE=%STOCKMIND_API_HOME%\STOCKMIND_API_config.txt"
set CFG_OPTS=
if exist "%CFG_FILE%" (
  FOR /F "tokens=* eol=# usebackq delims=" %%i IN ("%CFG_FILE%") DO (
    set DO_NOT_REUSE_ME=%%i
    rem ZOMG (Part #2) WE use !! here to delay the expansion of
    rem CFG_OPTS, otherwise it remains "" for this loop.
    set CFG_OPTS=!CFG_OPTS! !DO_NOT_REUSE_ME!
  )
)

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem Detect if this java is ok to use.
for /F %%j in ('"%_JAVACMD%" -version  2^>^&1') do (
  if %%~j==java set JAVAINSTALLED=1
  if %%~j==openjdk set JAVAINSTALLED=1
)

rem BAT has no logical or, so we do it OLD SCHOOL! Oppan Redmond Style
set JAVAOK=true
if not defined JAVAINSTALLED set JAVAOK=false

if "%JAVAOK%"=="false" (
  echo.
  echo A Java JDK is not installed or can't be found.
  if not "%JAVA_HOME%"=="" (
    echo JAVA_HOME = "%JAVA_HOME%"
  )
  echo.
  echo Please go to
  echo   http://www.oracle.com/technetwork/java/javase/downloads/index.html
  echo and download a valid Java JDK and install before running stockmind-api.
  echo.
  echo If you think this message is in error, please check
  echo your environment variables to see if "java.exe" and "javac.exe" are
  echo available via JAVA_HOME or PATH.
  echo.
  if defined DOUBLECLICKED pause
  exit /B 1
)


rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%
if "!_JAVA_OPTS!"=="" set _JAVA_OPTS=!CFG_OPTS!

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
rem "-J" is stripped, "-D" is left as is, and everything is appended to JAVA_OPTS
set _JAVA_PARAMS=
set _APP_ARGS=

:param_loop
call set _PARAM1=%%1
set "_TEST_PARAM=%~1"

if ["!_PARAM1!"]==[""] goto param_afterloop


rem ignore arguments that do not start with '-'
if "%_TEST_PARAM:~0,1%"=="-" goto param_java_check
set _APP_ARGS=!_APP_ARGS! !_PARAM1!
shift
goto param_loop

:param_java_check
if "!_TEST_PARAM:~0,2!"=="-J" (
  rem strip -J prefix
  set _JAVA_PARAMS=!_JAVA_PARAMS! !_TEST_PARAM:~2!
  shift
  goto param_loop
)

if "!_TEST_PARAM:~0,2!"=="-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1,*" %%G in ("!_TEST_PARAM!") DO (
    if not ["%%H"] == [""] (
      set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      call set _PARAM1=%%1=%%2
      set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
      shift
    )
  )
) else (
  if "!_TEST_PARAM!"=="-main" (
    call set CUSTOM_MAIN_CLASS=%%2
    shift
  ) else (
    set _APP_ARGS=!_APP_ARGS! !_PARAM1!
  )
)
shift
goto param_loop
:param_afterloop

set _JAVA_OPTS=!_JAVA_OPTS! !_JAVA_PARAMS!
:run

set "APP_CLASSPATH=%APP_LIB_DIR%\..\conf\;%APP_LIB_DIR%\com.clluc.stockmind-api-2.0.0-sans-externalized.jar;%APP_LIB_DIR%\com.clluc.primary-ports-2.0.0.jar;%APP_LIB_DIR%\com.clluc.model-2.0.0.jar;%APP_LIB_DIR%\com.clluc.secondary-ports-2.0.0.jar;%APP_LIB_DIR%\com.clluc.core-2.0.0.jar;%APP_LIB_DIR%\com.clluc.postgres-secondary-adapter-2.0.0.jar;%APP_LIB_DIR%\com.clluc.ethereum-secondary-adapter-2.0.0.jar;%APP_LIB_DIR%\com.clluc.twitter-secondary-adapter-2.0.0.jar;%APP_LIB_DIR%\com.clluc.controllers-2.0.0.jar;%APP_LIB_DIR%\org.scala-lang.scala-library-2.12.3.jar;%APP_LIB_DIR%\com.typesafe.scala-logging.scala-logging_2.12-3.5.0.jar;%APP_LIB_DIR%\com.typesafe.play.play-guice_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.build-link-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-exceptions-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-netty-utils-2.6.6.jar;%APP_LIB_DIR%\org.slf4j.slf4j-api-1.7.25.jar;%APP_LIB_DIR%\org.slf4j.jul-to-slf4j-1.7.25.jar;%APP_LIB_DIR%\org.slf4j.jcl-over-slf4j-1.7.25.jar;%APP_LIB_DIR%\com.typesafe.play.play-streams_2.12-2.6.6.jar;%APP_LIB_DIR%\org.reactivestreams.reactive-streams-1.0.1.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-stream_2.12-2.5.4.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-actor_2.12-2.5.4.jar;%APP_LIB_DIR%\com.typesafe.config-1.3.1.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-java8-compat_2.12-0.8.0.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-parser-combinators_2.12-1.0.6.jar;%APP_LIB_DIR%\com.typesafe.play.twirl-api_2.12-1.3.12.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-xml_2.12-1.0.6.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-slf4j_2.12-2.5.4.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-core-2.8.10.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-annotations-2.8.10.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-databind-2.8.10.jar;%APP_LIB_DIR%\com.fasterxml.jackson.datatype.jackson-datatype-jdk8-2.8.10.jar;%APP_LIB_DIR%\com.fasterxml.jackson.datatype.jackson-datatype-jsr310-2.8.10.jar;%APP_LIB_DIR%\commons-codec.commons-codec-1.10.jar;%APP_LIB_DIR%\com.typesafe.play.play-json_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-functional_2.12-2.6.6.jar;%APP_LIB_DIR%\org.scala-lang.scala-reflect-2.12.3.jar;%APP_LIB_DIR%\org.typelevel.macro-compat_2.12-1.1.1.jar;%APP_LIB_DIR%\joda-time.joda-time-2.9.9.jar;%APP_LIB_DIR%\com.google.errorprone.error_prone_annotations-2.0.18.jar;%APP_LIB_DIR%\com.google.j2objc.j2objc-annotations-1.1.jar;%APP_LIB_DIR%\org.codehaus.mojo.animal-sniffer-annotations-1.14.jar;%APP_LIB_DIR%\io.jsonwebtoken.jjwt-0.7.0.jar;%APP_LIB_DIR%\org.apache.commons.commons-lang3-3.6.jar;%APP_LIB_DIR%\javax.transaction.jta-1.1.jar;%APP_LIB_DIR%\javax.inject.javax.inject-1.jar;%APP_LIB_DIR%\com.google.inject.guice-4.1.0.jar;%APP_LIB_DIR%\aopalliance.aopalliance-1.0.jar;%APP_LIB_DIR%\com.google.inject.extensions.guice-assistedinject-4.1.0.jar;%APP_LIB_DIR%\org.joda.joda-convert-1.8.1.jar;%APP_LIB_DIR%\org.typelevel.cats_2.12-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-macros_2.12-0.9.0.jar;%APP_LIB_DIR%\com.github.mpilquist.simulacrum_2.12-0.10.0.jar;%APP_LIB_DIR%\org.typelevel.machinist_2.12-0.6.1.jar;%APP_LIB_DIR%\org.typelevel.cats-kernel_2.12-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-kernel-laws_2.12-0.9.0.jar;%APP_LIB_DIR%\org.scalacheck.scalacheck_2.12-1.13.4.jar;%APP_LIB_DIR%\org.scala-sbt.test-interface-1.0.jar;%APP_LIB_DIR%\org.typelevel.discipline_2.12-0.7.2.jar;%APP_LIB_DIR%\org.typelevel.catalysts-platform_2.12-0.0.5.jar;%APP_LIB_DIR%\org.typelevel.catalysts-macros_2.12-0.0.5.jar;%APP_LIB_DIR%\org.typelevel.cats-core_2.12-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-laws_2.12-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-free_2.12-0.9.0.jar;%APP_LIB_DIR%\org.typelevel.cats-jvm_2.12-0.9.0.jar;%APP_LIB_DIR%\com.google.guava.guava-23.0.jar;%APP_LIB_DIR%\io.circe.circe-core_2.12-0.8.0.jar;%APP_LIB_DIR%\io.circe.circe-numbers_2.12-0.8.0.jar;%APP_LIB_DIR%\io.circe.circe-generic_2.12-0.8.0.jar;%APP_LIB_DIR%\com.chuusai.shapeless_2.12-2.3.2.jar;%APP_LIB_DIR%\io.circe.circe-shapes_2.12-0.8.0.jar;%APP_LIB_DIR%\io.circe.circe-parser_2.12-0.8.0.jar;%APP_LIB_DIR%\io.circe.circe-jawn_2.12-0.8.0.jar;%APP_LIB_DIR%\org.spire-math.jawn-parser_2.12-0.10.4.jar;%APP_LIB_DIR%\io.circe.circe-optics_2.12-0.8.0.jar;%APP_LIB_DIR%\com.github.julien-truffaut.monocle-core_2.12-1.4.0.jar;%APP_LIB_DIR%\org.scalaz.scalaz-core_2.12-7.2.8.jar;%APP_LIB_DIR%\org.tpolecat.doobie-core-cats_2.12-0.4.4.jar;%APP_LIB_DIR%\com.lihaoyi.sourcecode_2.12-0.1.3.jar;%APP_LIB_DIR%\co.fs2.fs2-core_2.12-0.9.6.jar;%APP_LIB_DIR%\co.fs2.fs2-cats_2.12-0.3.0.jar;%APP_LIB_DIR%\org.tpolecat.doobie-postgres-cats_2.12-0.4.4.jar;%APP_LIB_DIR%\org.postgresql.postgresql-42.1.1.jar;%APP_LIB_DIR%\com.typesafe.play.play-ahc-ws_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-ws_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-ws-standalone_2.12-1.1.2.jar;%APP_LIB_DIR%\com.typesafe.ssl-config-core_2.12-0.2.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-ws-standalone-xml_2.12-1.1.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-ws-standalone-json_2.12-1.1.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-ahc-ws-standalone_2.12-1.1.2.jar;%APP_LIB_DIR%\com.typesafe.play.cachecontrol_2.12-1.1.2.jar;%APP_LIB_DIR%\com.typesafe.play.shaded-asynchttpclient-1.1.2.jar;%APP_LIB_DIR%\com.typesafe.play.shaded-oauth-1.1.2.jar;%APP_LIB_DIR%\javax.cache.cache-api-1.0.0.jar;%APP_LIB_DIR%\org.twitter4j.twitter4j-core-4.0.4.jar;%APP_LIB_DIR%\com.mohiva.play-silhouette_2.12-5.0.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-server_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.filters-helpers_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-logback_2.12-2.6.6.jar;%APP_LIB_DIR%\ch.qos.logback.logback-classic-1.2.3.jar;%APP_LIB_DIR%\ch.qos.logback.logback-core-1.2.3.jar;%APP_LIB_DIR%\com.typesafe.play.play-akka-http-server_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-http-core_2.12-10.0.10.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-parsing_2.12-10.0.10.jar;%APP_LIB_DIR%\com.typesafe.play.play-cache_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-openid_2.12-2.6.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-json-joda_2.12-2.6.6.jar;%APP_LIB_DIR%\com.atlassian.jwt.jwt-core-1.6.1.jar;%APP_LIB_DIR%\com.nimbusds.nimbus-jose-jwt-3.6.jar;%APP_LIB_DIR%\net.jcip.jcip-annotations-1.0.jar;%APP_LIB_DIR%\net.minidev.json-smart-1.1.1.jar;%APP_LIB_DIR%\org.bouncycastle.bcprov-jdk15on-1.51.jar;%APP_LIB_DIR%\commons-io.commons-io-2.4.jar;%APP_LIB_DIR%\org.bouncycastle.bcpkix-jdk15on-1.51.jar;%APP_LIB_DIR%\com.atlassian.jwt.jwt-api-1.6.1.jar;%APP_LIB_DIR%\com.mohiva.play-silhouette-persistence_2.12-5.0.2.jar;%APP_LIB_DIR%\com.typesafe.play.play-ehcache_2.12-2.6.6.jar;%APP_LIB_DIR%\net.sf.ehcache.ehcache-2.10.4.jar;%APP_LIB_DIR%\org.ehcache.jcache-1.0.1.jar;%APP_LIB_DIR%\com.typesafe.play.play-mailer_2.12-6.0.1.jar;%APP_LIB_DIR%\org.apache.commons.commons-email-1.5.jar;%APP_LIB_DIR%\com.sun.mail.javax.mail-1.5.6.jar;%APP_LIB_DIR%\javax.activation.activation-1.1.jar;%APP_LIB_DIR%\com.mohiva.play-silhouette-password-bcrypt_2.12-5.0.2.jar;%APP_LIB_DIR%\de.svenkubiak.jBCrypt-0.4.1.jar;%APP_LIB_DIR%\com.mohiva.play-silhouette-crypto-jca_2.12-5.0.2.jar;%APP_LIB_DIR%\net.codingwell.scala-guice_2.12-4.1.0.jar;%APP_LIB_DIR%\com.google.inject.extensions.guice-multibindings-4.1.0.jar;%APP_LIB_DIR%\com.google.code.findbugs.jsr305-3.0.1.jar;%APP_LIB_DIR%\com.iheart.ficus_2.12-1.4.1.jar;%APP_LIB_DIR%\com.clluc.stockmind-api-2.0.0-assets.jar"
set "APP_MAIN_CLASS=play.core.server.ProdServerStart"

if defined CUSTOM_MAIN_CLASS (
    set MAIN_CLASS=!CUSTOM_MAIN_CLASS!
) else (
    set MAIN_CLASS=!APP_MAIN_CLASS!
)

rem Call the application and pass all arguments unchanged.
"%_JAVACMD%" !_JAVA_OPTS! !STOCKMIND_API_OPTS! -cp "%APP_CLASSPATH%" %MAIN_CLASS% !_APP_ARGS!

@endlocal


:end

exit /B %ERRORLEVEL%
