package org.alexeyn.news.scraper

import cats.effect.{Sync, Timer}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import org.alexeyn.news.repo.NewsRepo
import scala.concurrent.duration.FiniteDuration

class ScrapeService[F[_]: Sync](
    scraper: Scraper[F],
    repo: NewsRepo[F],
    interval: Option[FiniteDuration]
)(implicit L: Logger[F], T: Timer[F]) {

  def scrape: F[Unit] =
    for {
      headlines <- scraper.headlines()
      count <- repo.upsert(headlines)
      _ <- L.info(s"Upserted record count: $count")
      _ <- interval match {
        case Some(i) => T.sleep(i) *> scrape
        case None    => L.info(s"Stopping scrapper...")
      }
    } yield ()
}
