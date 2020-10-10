package org.alexeyn.news

import cats.effect.Sync
import doobie._
import doobie.implicits._

object SchemaCreator {
  def create[F[_]: Sync](
      xa: Transactor[F],
      user: String,
      database: String
  ): F[Unit] = {
    val script = Fragment.const(s"""
       |GRANT CONNECT ON DATABASE $database TO $user;
       |
       |CREATE SCHEMA IF NOT EXISTS news AUTHORIZATION $user;
       |
       |CREATE TABLE news.headline (
       |  link VARCHAR PRIMARY KEY,
       |  title VARCHAR NOT NULL
       |);       
       |""".stripMargin)
    script.update.run.map(_ => ()).transact(xa)
  }
}