package org.alexeyn.news.schema

import cats.effect._
import org.alexeyn.news.Headline
import org.alexeyn.news.repo.NewsRepo
import sangria.schema._

object NewsType {

  def apply[F[_]: Effect]: ObjectType[NewsRepo[F], Headline] =
    ObjectType(
      name = "News",
      fieldsFn = () =>
        fields(
          Field(
            name = "title",
            fieldType = StringType,
            resolve = _.value.title
          ),
          Field(
            name = "link",
            fieldType = StringType,
            resolve = _.value.link
          )
        )
    )

}
