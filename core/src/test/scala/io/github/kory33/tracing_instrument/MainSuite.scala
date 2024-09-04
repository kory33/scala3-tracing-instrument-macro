/*
 * Copyright 2024 Ryosuke Kondo
 *
 * SPDX-License-Identifier: MIT
 */

package io.github.kory33.tracing_instrument

import munit.CatsEffectSuite

class MainSuite extends CatsEffectSuite {

  test("Main should exit succesfully") {
    val main = Main.run.attempt
    assertIO(main, Right(()))
  }

}
