package io.github.kory33.tracing_instrument.otel4s

import io.github.kory33.tracing_instrument.core.macros.MacrosUtil
import scala.compiletime.summonInline
import scala.quoted.*
import org.typelevel.otel4s.trace.Tracer

object encloseInSpan {
  inline def apply[F[_], A](f: F[A]): F[A] = ${
    applyImplWithKnownF[F, A]('f)
  }

  def applyImplWithKnownF[F[_]: Type, A: Type](using
      Quotes
  )(f: Expr[F[A]]): Expr[F[A]] = {
    import quotes.reflect.*

    val spanName = MacrosUtil.enclosingDefDef.getOrElse {
      report.errorAndAbort(
        "Failed to find the enclosing method definition."
      )
    }.name

    '{
      summonInline[Tracer[F]]
        .span(
          name = ${ Expr(spanName) },
          // TODO: look at parameters of the enclosing method, parameters with Show instance
          //       and construct attributes from them
          attributes = Seq()*
        )
        .use(_ => $f)
    }
  }
}
