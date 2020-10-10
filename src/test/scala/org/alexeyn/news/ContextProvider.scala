package org.alexeyn.news

import scala.concurrent.ExecutionContext
import cats.effect.IO
import cats.effect.Timer
import cats.effect.ContextShift

trait ContextProvider {
  implicit lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit lazy val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
}
