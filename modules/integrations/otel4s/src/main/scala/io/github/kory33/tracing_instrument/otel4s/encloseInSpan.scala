package io.github.kory33.tracing_instrument.otel4s

import scala.compiletime.summonInline
import scala.quoted.*
import org.typelevel.otel4s.trace.Tracer

object encloseInSpan {
  inline def apply[F[_], A](f: F[A]): F[A] = ${
    applyImplWithKnownTracerF[F, A]('f)
  }

  def applyImplWithKnownTracerF[F[_]: Type, A: Type](using
      Quotes
  )(f: Expr[F[A]]): Expr[F[A]] = {
    import quotes.reflect.*

    '{
      summonInline[Tracer[F]]
        .span(${ Expr(Symbol.spliceOwner.name) })
        .use(_ => $f)
    }
  }
}
