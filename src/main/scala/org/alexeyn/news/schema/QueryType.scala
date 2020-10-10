// Copyright (c) 2018 by Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.alexeyn.news.schema

import cats.effect._
import cats.effect.implicits._
import org.alexeyn.news.repo.NewsRepo
import sangria.schema._

object QueryType {

  def apply[F[_]: Effect]: ObjectType[NewsRepo[F], Unit] =
    ObjectType(
      name = "Query",
      fields = fields(
        Field(
          name = "news",
          fieldType = ListType(NewsType[F]),
          description = Some("Returns all news."),
          resolve = c => c.ctx.fetchAll.toIO.unsafeToFuture
        )
      )
    )

  def schema[F[_]: Effect]: Schema[NewsRepo[F], Unit] =
    Schema(QueryType[F])

}
