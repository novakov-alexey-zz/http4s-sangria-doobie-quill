package org.alexeyn.news

import cats.effect._
import monix.eval.{Task, _}

object CatsIOMain extends NewsModule with IOApp {
  def run(args: List[String]): IO[ExitCode] =
    tasks[IO].use {
      case (server, scraper) => IO.race(server, scraper).map(_.merge)
    }
}

object MonixMain extends NewsModule with TaskApp {
  def run(args: List[String]): Task[ExitCode] =
    tasks[Task].use {
      case (server, scraper) => Task.race(server, scraper).map(_.merge)
    }
}
