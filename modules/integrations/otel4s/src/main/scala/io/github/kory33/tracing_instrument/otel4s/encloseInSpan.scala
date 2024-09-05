package io.github.kory33.tracing_instrument.otel4s

import io.github.kory33.tracing_instrument.core.cats.macros.CatsMacrosUtil
import io.github.kory33.tracing_instrument.core.macros.MacrosUtil
import scala.compiletime.summonInline
import scala.quoted.*
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.Attribute

object encloseInSpan {
  inline def apply[F[_], A](f: F[A]): F[A] = ${
    applyImplWithKnownF[F, A]('f)
  }

  def applyImplWithKnownF[F[_]: Type, A: Type](using
      Quotes
  )(f: Expr[F[A]]): Expr[F[A]] = {
    import quotes.reflect.*

    val enclosingDefDef = MacrosUtil.enclosingDefDef.getOrElse {
      report.errorAndAbort(
        "Failed to find the enclosing method definition."
      )
    }

    val spanName = enclosingDefDef.name
    val attributesExpr: Expr[Seq[Attribute[?]]] = {
      val enclosingDefDefShowableParams =
        CatsMacrosUtil.defDefParametersWithShowInstances(enclosingDefDef)

      Expr.ofSeq {
        // TODO: parameters with primitive types (those with AttributeKey.KeySelect)
        //       can be directly made as attributes
        //       (since AttributeKey.KeySelect implies Show, such parameters are already
        //        registered as attributes, but by prioritizing AttributeKey.KeySelect instances
        //        we can provide slightly more strict attribute key type for them)
        enclosingDefDefShowableParams.map { case (valDef, showInstance) =>
          '{
            Attribute[String](
              ${ Expr(valDef.name) },
              ${
                (Select
                  .unique(showInstance, "show"): Term)
                  .appliedTo(Ref(valDef.symbol): Term)
                  .asExprOf[String]
              }
            )
          }
        }
      }
    }

    '{
      summonInline[Tracer[F]]
        .span(
          name = ${ Expr(spanName) },
          attributes = ${ attributesExpr }*
        )
        .use(_ => $f)
    }
  }
}
