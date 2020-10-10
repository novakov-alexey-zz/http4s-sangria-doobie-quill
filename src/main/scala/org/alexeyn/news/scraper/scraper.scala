package org.alexeyn.news.scraper

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Document
import org.alexeyn.news.Headline
import org.alexeyn.news.refined.URL

trait Scraper[F[_]] {
  def headlines(): F[List[Headline]]
}

private[news] class NYTimes[F[_]: Sync](
    url: URL,
    browser: Browser,
    getDoc: Browser => F[Document]
) extends Scraper[F] {

  def headlines(): F[List[Headline]] =
    for {
      doc <- getDoc(browser)
      values <- Sync[F].delay(extractValues(doc))
      list = values.map {
        case (title, link) =>
          val fullUrl =
            if (link.startsWith(url.value)) link else url.value + link
          Headline(title, fullUrl)
      }
    } yield list

  private def extractValues(doc: Document): List[(String, String)] =
    doc >> elementList("a:has(h2)") >> (text("h2"), attr("href"))
}

object NYTimes {
  def apply[F[_]: Sync](url: URL): Scraper[F] =
    new NYTimes[F](
      url,
      JsoupBrowser(),
      browser => Sync[F].delay(browser.get(url.value))
    )
}
