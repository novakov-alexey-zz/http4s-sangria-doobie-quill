package org.alexeyn.news

import cats.effect._
import io.circe.generic.auto._
import io.circe.parser._
import org.alexeyn.news.repo.NewsRepo
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.http4s.{Method, Request, _}
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class RoutesIT
    extends AnyPropSpec
    with ScalaCheckPropertyChecks
    with Matchers
    with ContextProvider {

  def headlinesGen: Gen[List[Headline]] =
    for {
      headline <- for {
        name <- Gen.alphaStr
        password <- Gen.alphaStr
      } yield Headline(name, password)
      list <- Gen.listOf(headline)
    } yield list

  property("returns news headlines") {
    val mod = new NewsModule() {}
    val query =
      parse(
        """{"operationName":null,"variables":{},"query":"{news {title link }}"}"""
      ).getOrElse(sys.error("Failed to parse to GraphQL query"))
    val request =
      Request[IO](method = Method.POST, uri = Uri(path = "/graphql"))
        .withEntity(query)
    val blocker = Blocker[IO]

    forAll(headlinesGen) { news: List[Headline] =>
      //given
      val repo = new NewsRepo[IO] {
        override def fetchAll: IO[List[Headline]] =
          IO.pure(
            news
          )

        override def upsert(headlines: List[Headline]): IO[Int] = ???
      }

      val routes = for {
        b <- blocker
        rts = mod.httpRoutes[IO](repo, b)
      } yield rts.orNotFound

      //when
      val result = routes.use { rts =>
        for {
          res <- rts.run(request)
          payload <- res.as[NewsResponse]
        } yield {
          //then
          payload.data.news.length should ===(news.length)
          payload.data.news should ===(news)
        }
      }

      result.unsafeRunSync()
    }
  }
}
