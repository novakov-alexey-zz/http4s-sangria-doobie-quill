package org.alexeyn.news

import _root_.sangria.schema._
import cats.effect._
import cats.implicits._
import doobie.hikari._
import doobie.util.ExecutionContexts
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.alexeyn.news.api.{GraphQL, GraphQLRoutes, PlaygroundRoutes}
import org.alexeyn.news.refined.URL
import org.alexeyn.news.repo._
import org.alexeyn.news.sangria.SangriaGraphQL
import org.alexeyn.news.schema._
import org.alexeyn.news.scraper.{NYTimes, ScrapeService, Scraper}
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.FiniteDuration

trait NewsModule {

  def transactor[F[_]: Async: ContextShift](
      blocker: Blocker,
      cfg: JdbcConfig
  ): Resource[F, HikariTransactor[F]] =
    ExecutionContexts.fixedThreadPool[F](10).flatMap { ce =>
      HikariTransactor.newHikariTransactor(
        cfg.driver.value,
        cfg.url.value,
        cfg.user.value,
        cfg.password.value,
        ce,
        blocker
      )
    }

  def graphQL[F[_]: Effect: ContextShift](
      repo: NewsRepo[F],
      blockingContext: ExecutionContext
  ): GraphQL[F] =
    SangriaGraphQL[F](
      Schema(
        query = QueryType[F]
      ),
      repo.pure[F],
      blockingContext
    )

  def server[F[_]: ConcurrentEffect: ContextShift: Timer](
      routes: HttpRoutes[F]
  ): Resource[F, Server[F]] =
    BlazeServerBuilder[F](global)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .resource

  def graphQLServer[F[_]: ConcurrentEffect: ContextShift: Timer: Logger](
      repo: NewsRepo[F],
      b: Blocker
  ): Resource[F, Server[F]] = {
    val rts = httpRoutes(repo, b)
    server[F](rts)
  }

  def httpRoutes[F[_]: ConcurrentEffect: ContextShift: Timer](
      repo: NewsRepo[F],
      b: Blocker
  ): HttpRoutes[F] = {
    val gql = graphQL[F](repo, b.blockingContext)
    GraphQLRoutes[F](gql) <+> PlaygroundRoutes(b)
  }

  def scraper[F[_]: Sync](url: URL): Scraper[F] =
    NYTimes[F](url)

  def scrapeTask[F[_]: Sync: Logger: Timer](
      repo: NewsRepo[F],
      cfg: ScrapeConfig
  ): F[Unit] =
    for {
      scraper <- scraper(cfg.nytimesUrl).pure[F]
      _ <- scrapeService(scraper, repo, Some(cfg.interval)).scrape
    } yield ()

  def scrapeService[F[_]: Sync: Logger: Timer](
      scraper: Scraper[F],
      repo: NewsRepo[F],
      interval: Option[FiniteDuration]
  ) =
    new ScrapeService[F](scraper, repo, interval)

  def tasks[F[_]: ConcurrentEffect: ContextShift: Timer: Async]
      : Resource[F, (F[ExitCode], F[ExitCode])] = {
    implicit val log = Slf4jLogger.getLogger[F]
    for {
      cfg <- Resource.liftF(loadConfig)
      b <- Blocker[F]
      xa <- transactor[F](b, cfg.db)
      repo = NewsRepo.fromTransactor(xa)
      server = graphQLServer[F](repo, b)
        .use(_ =>
          Async[F]
            .async((_: Either[Throwable, Nothing] => Unit) => ())
            .as(ExitCode.Error) <* log.info("HTTP server stopped")
        )
      scraper = scrapeTask[F](repo, cfg.scrape).as(ExitCode.Error)
    } yield (server, scraper)
  }

  def loadConfig[F[_]: Sync]: F[AppConfig] =
    AppConfig.load[F]
}
