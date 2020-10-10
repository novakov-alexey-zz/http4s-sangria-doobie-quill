lazy val Http4sVersion = "0.21.5"
lazy val CirceVersion = "0.13.0"
lazy val Specs2Version = "4.10.0"
lazy val LogbackVersion = "1.2.3"
lazy val SangriaVersion = "1.4.2"
lazy val SangriaCirceVersion = "1.3.0"
lazy val Log4CatsSlf4jVersion = "1.0.1"
lazy val DoobieVersion = "0.9.0"
lazy val CatsEffectVersion = "2.1.3"
lazy val ScalaScraperVersion = "2.2.0"
lazy val RefinedPureconfigVersion = "0.9.4"
lazy val PureConfigVersion = "0.10.1"
lazy val MonixVersion = "3.2.2"
lazy val MonixCatsVersion = "2.3.3"
lazy val ScalaTestVersion = "3.2.0"
lazy val ScalaTestCheckVersion = "3.1.0.0-RC2"
lazy val TestContainersVersion = "0.20.0"
lazy val TestContainersPostgresVersion = "1.9.1"

lazy val root = (project in file("."))
  .settings(
    organization := "org.alexeyn",
    name := "news",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.12",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,      
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "com.softwaremill.sttp.client" %% "http4s-backend" % "2.2.5",
      "com.softwaremill.sttp.client" %% "circe" % "2.2.5",
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "io.circe" %% "circe-optics" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.sangria-graphql" %% "sangria" % SangriaVersion,
      "org.sangria-graphql" %% "sangria-circe" % SangriaCirceVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % Log4CatsSlf4jVersion,
      "net.ruippeixotog" %% "scala-scraper" % ScalaScraperVersion,
      "eu.timepit" %% "refined-pureconfig" % RefinedPureconfigVersion,
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "org.tpolecat" %% "doobie-quill" % DoobieVersion,
      "io.monix" %% "monix" % MonixVersion,
      "io.monix" %% "monix-cats" % MonixCatsVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.scalatestplus" %% "scalatestplus-scalacheck" % ScalaTestCheckVersion % Test,
      "com.dimafeng" %% "testcontainers-scala" % TestContainersVersion % Test,
      "org.testcontainers" % "postgresql" % TestContainersPostgresVersion % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / watchBeforeCommand := Watch.clearScreen
