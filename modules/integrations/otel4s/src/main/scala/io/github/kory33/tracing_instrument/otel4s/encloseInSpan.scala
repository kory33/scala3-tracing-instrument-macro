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

    val spanName = constructSpanNameFromAncestorsOf(Symbol.spliceOwner)
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

  private def constructSpanNameFromAncestorsOf(using
      Quotes
  )(symbol: quotes.reflect.Symbol): String = {
    import quotes.reflect.*

    val ownerAncestors = {
      val ancestors = List.newBuilder[Symbol]
      try {
        var owner = symbol
        while (true) {
          ancestors += owner
          owner = owner.owner
        }
      } catch {
        case _: Throwable =>
      }

      ancestors
        .result()
        .filter(s => s.isDefDef || s.isClassDef || s.isPackageDef)
        .filter(s => s != defn.RootPackage && s != defn.RootClass)
    }

    ownerAncestors.reverse
      .foldLeft(
        ("" /* accum */, Option.empty[String] /* next delimiter */ )
      ) { (accumPair, nextAncestor) =>
        val (accum, nextDelimiter) = accumPair

        val nextDelimiterAfterThis = Some {
          if (nextAncestor.isDefDef || nextAncestor.isPackageDef) "."
          else if (nextAncestor.isClassDef) "#"
          else ""
        }

        (
          accum + nextDelimiter.getOrElse("") + nextAncestor.name,
          nextDelimiterAfterThis
        )
      }
      ._1
  }
}
