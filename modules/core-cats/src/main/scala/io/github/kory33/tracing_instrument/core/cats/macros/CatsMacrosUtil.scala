package io.github.kory33.tracing_instrument.core.cats.macros

import scala.quoted.*
import cats.Show.ContravariantShow
import io.github.kory33.tracing_instrument.core.macros.MacrosUtil

object CatsMacrosUtil {
  def defDefParametersWithShowInstances(using
      Quotes
  )(defDef: quotes.reflect.DefDef): List[
    (
        quotes.reflect.ValDef,
        quotes.reflect.Term /* ContravariantShow instance that is able to show the ValDef */
    )
  ] = {
    import quotes.reflect.*

    MacrosUtil.defDefValParameters(defDef).flatMap { valDef =>
      val contravariantShowInstance = valDef.tpt.tpe.asType match {
        case '[t] => Expr.summon[ContravariantShow[t]].map(_.asTerm)
      }

      contravariantShowInstance.map(valDef -> _)
    }
  }
}
