package org.alexeyn.news

import cats.effect.{IO, Sync, _}
import cats.implicits._
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.{Config, ConfigFactory}
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.alexeyn.news.refined.URL
import org.alexeyn.news.repo.NewsRepo
import org.alexeyn.news.scraper.{NYTimes, ScrapeService, Scraper}
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client.circe._
import sttp.client.http4s.Http4sBackend
import sttp.client.{NothingT, SttpBackend, _}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class E2ETest
    extends AnyFlatSpec
    with Matchers
    with ForAllTestContainer
    with Eventually
    with BeforeAndAfter
    with ContextProvider {

  val client = BlazeClientBuilder[IO](ExecutionContext.global)
  val blocker = Blocker.liftExecutionContext(ExecutionContext.global)

  implicit val backend: SttpBackend[IO, Nothing, NothingT] =
    Http4sBackend
      .usingClientBuilder(client, blocker)
      .allocated
      .unsafeRunSync()
      ._1

  override val container = PostgreSQLContainer("postgres")

  class TestModule(jdbc: Config) extends NewsModule {
    override def scraper[F[_]: Sync](url: URL): Scraper[F] =
      new NYTimes[F](
        url,
        JsoupBrowser(),
        browser =>
          Sync[F].delay(
            (browser.parseFile(
              "src/test/resources/html/nytimes-200820-noscript.html"
            ))
          )
      )

    override def loadConfig[F[_]: Sync]: F[AppConfig] =
      for {
        cfg <- AppConfig.parseConfig[F]
        scrape <- AppConfig.loadScrape[F](cfg)
        db <- AppConfig.loadStorage[F](jdbc.withFallback(cfg))
      } yield AppConfig(db, scrape)

    override def scrapeService[F[_]: Sync: Logger: Timer](
        scraper: Scraper[F],
        repo: NewsRepo[F],
        interval: Option[FiniteDuration]
    ) =
      new ScrapeService[F](scraper, repo, None) // one-time job
  }

  lazy val containerCfg: Config = ConfigFactory.load(
    ConfigFactory
      .parseMap(
        Map(
          "port" -> container.mappedPort(5432),
          "url" -> (container.jdbcUrl + "?currentSchema=news"),
          "user" -> container.username,
          "password" -> container.password
        ).asJava
      )
      .atKey("storage")
  )

  it should "scrape and serve news" in {
    //given
    val mod = new TestModule(containerCfg)
    createSchema(mod).unsafeRunSync()

    val tasks = mod.tasks[IO]
    val scrapers = tasks
      .use {
        //when
        case (_, scraper) => List.fill(3)(scraper).parSequence
      }
    scrapers.unsafeRunSync()

    val cancelable = tasks
      .use {
        case (server, _) =>
          //when
          server
      }
      .unsafeRunCancelable {
        case Left(t)   => throw t
        case Right(ec) => println(s"exit code: $ec")
      }

    val request = basicRequest
      .body(
        """{"operationName":null,"variables":{},"query":"{news {title link }}"}"""
      )
      .post(uri"http://0.0.0.0:8080/graphql")
      .response(asJson[NewsResponse])
      .send()

    eventually {
      request
        .map { response =>
          response.body match {
            //then
            case Left(err)   => fail(err.fillInStackTrace())
            case Right(body) => body.data.news.length should ===(32)
          }
        }
        .unsafeRunSync()
    }

    cancelable.unsafeRunSync()
  }

  private def createSchema(mod: TestModule) = {
    val transactor = for {
      blocker <- Blocker[IO]
      jdbcCfg <- Resource.liftF(for {
        origConfig <- AppConfig.parseConfig[IO]
        resolved <- IO(containerCfg.withFallback(origConfig).resolve())
        jdbcCfg <- AppConfig.loadStorage[IO](resolved)
        _ = println(s"loaded jdbc config: $jdbcCfg")
      } yield jdbcCfg)
      xa <- mod.transactor[IO](blocker, jdbcCfg)
    } yield (xa, jdbcCfg)

    transactor
      .use {
        case (xa, jdbcCfg) =>
          SchemaCreator.create(xa, jdbcCfg.user.value, jdbcCfg.user.value)
      }
  }
}
