/*
 * Copyright 2024 Ryosuke Kondo
 *
 * SPDX-License-Identifier: MIT
 */

package io.github.kory33.tracing_instrument

import cats.effect.IO
import cats.effect.IOApp

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    IO.println("Hello sbt-typelevel!")
}
