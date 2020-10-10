package org.alexeyn.news.repo

import cats.effect.Sync
import cats.implicits._
import doobie.quill.DoobieContext
import doobie._
import doobie.implicits._
import doobie.quill.DoobieContext
import io.chrisdavenport.log4cats.Logger
import io.getquill.{idiom => _, _}
import org.alexeyn.news.Headline

trait NewsRepo[F[_]] {
  def fetchAll: F[List[Headline]]

  def upsert(headlines: List[Headline]): F[Int]
}

object NewsRepo {

  def fromTransactor[F[_]: Sync: Logger](xa: Transactor[F]): NewsRepo[F] =
    new NewsRepo[F] {
      val dc = new DoobieContext.Postgres(Literal)

      import dc._

      def fetchAll: F[List[Headline]] = {
        val q = quote(query[Headline])
        Logger[F].debug(s"NewsRepo.fetchAll") *> run(q).transact(xa)
      }

      override def upsert(headlines: List[Headline]): F[Int] = {
        val q = quote {
          liftQuery(headlines).foreach(e =>
            query[Headline]
              .insert(e)
              .onConflictUpdate(_.link)((t, e) => t.title -> e.title)
          )
        }
        run(q).transact(xa).map(_.length)
      }
    }

}
