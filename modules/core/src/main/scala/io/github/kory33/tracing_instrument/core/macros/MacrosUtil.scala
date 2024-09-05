/*
 * Copyright 2024 Ryosuke Kondo
 *
 * SPDX-License-Identifier: MIT
 */

package io.github.kory33.tracing_instrument.core.macros

import scala.annotation.tailrec
import scala.quoted.*

object MacrosUtil {
  def enclosingDefDef(using Quotes): Option[quotes.reflect.DefDef] = {
    import quotes.reflect.*

    @tailrec def searchAscending(
        symbol: quotes.reflect.Symbol
    ): Option[quotes.reflect.DefDef] = {
      symbol.tree match {
        case defDef: DefDef => Some(defDef)
        case _ =>
          val owner =
            try { symbol.owner }
            // when symbol does not have owner, symbol.owner throws so we catch it here
            catch { case _: Throwable => return None }

          searchAscending(owner)
      }
    }

    searchAscending(Symbol.spliceOwner)
  }

  def defDefValParameters(using
      Quotes
  )(defDef: quotes.reflect.DefDef): List[quotes.reflect.ValDef] = {
    import quotes.reflect.*

    defDef.paramss
      .map(_.params)
      .flatten
      .collect { case valDef: ValDef => valDef }
  }
}
