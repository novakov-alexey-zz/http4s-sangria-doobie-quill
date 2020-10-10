package org.alexeyn.news

import java.io.File

import cats.effect.Sync
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions}
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.string.{MatchesRegex, Url}
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import org.alexeyn.news.refined._
import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

object refined {
  type ConnectionTimeout =
    Int Refined Interval.OpenClosed[W.`0`.T, W.`100000`.T]
  type MaxPoolSize = Int Refined Interval.OpenClosed[W.`0`.T, W.`100`.T]
  type JdbcUrl = String Refined MatchesRegex[
    W.`"""jdbc:\\w+://[A-Za-z0-9\\.-]+:[0-9]{4,5}/\\w+\\?\\w+\\=\\w+"""`.T
  ]
  type URL = String Refined Url
}

final case class JdbcConfig(
    host: NonEmptyString,
    port: UserPortNumber,
    dbName: NonEmptyString,
    url: JdbcUrl,
    driver: NonEmptyString,
    user: NonEmptyString,
    password: NonEmptyString,
    connectionTimeout: ConnectionTimeout,
    maximumPoolSize: MaxPoolSize
) {
  override def toString: String =
    s"JdbcConfig($host, $port, $dbName, $url, $driver, $user, xxxxxxxxx, $connectionTimeout, $maximumPoolSize)"
}

final case class ScrapeConfig(nytimesUrl: URL, interval: FiniteDuration)

final case class AppConfig(db: JdbcConfig, scrape: ScrapeConfig)

object AppConfig {
  private val parseOptions =
    ConfigParseOptions.defaults().setAllowMissing(false)

  private val path =
    sys.env.getOrElse("APP_CONFIG_PATH", "src/main/resources/application.conf")

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  private def failureToString(failures: ConfigReaderFailures): String =
    failures.toList.map(_.toString).mkString(", ")

  private def loadConfigF[T, F[_]: Sync](key: String, config: Config)(implicit
      reader: Derivation[ConfigReader[T]]
  ) =
    Sync[F].fromEither(
      loadConfigWithFallback[T](config, key).left.map(err =>
        new RuntimeException(failureToString(err))
      )
    )

  def load[F[_]: Sync]: F[AppConfig] =
    for {
      config <- Sync[F].delay(
        ConfigFactory.parseFile(new File(path), parseOptions).resolve()
      )
      jdbc <- loadStorage(config)
      scrape <- loadScrape(config)
    } yield AppConfig(jdbc, scrape)

  def loadStorage[F[_]: Sync](config: Config): F[JdbcConfig] =
    loadConfigF[JdbcConfig, F]("storage", config)

  def loadScrape[F[_]: Sync](config: Config): F[ScrapeConfig] =
    loadConfigF[ScrapeConfig, F]("scraper", config)

  def parseConfig[F[_]: Sync]: F[Config] =
    Sync[F].delay(
      ConfigFactory.parseFile(new File(path), parseOptions).resolve()
    )
}
