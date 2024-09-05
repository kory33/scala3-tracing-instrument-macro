package io.github.kory33.tracing_instrument.otel4s

import scala.compiletime.summonInline
import scala.language.experimental
import scala.annotation.{MacroAnnotation, experimental}
import scala.quoted.*

@experimental class instrumentF extends MacroAnnotation {
  override def transform(using Quotes)(
      definition: quotes.reflect.Definition,
      companion: Option[quotes.reflect.Definition]
  ): List[quotes.reflect.Definition] = {
    import quotes.reflect.*

    definition match {
      case DefDef(defName, params, returnType, Some(definingTerm)) =>
        returnType match
          case Applied(fTree, List(retInnerType: TypeTree)) =>
            fTree.tpe.asType match
              case '[type f[_]; f] =>
                retInnerType.tpe.asType match
                  case '[ret] =>
                    List(
                      DefDef.copy(definition)(
                        defName,
                        params,
                        returnType,
                        Some(
                          '{ encloseInSpan[f, ret](${ definingTerm.asExprOf[f[ret]] }) }.asTerm
                        )
                      )
                    )
          case _ =>
            report.errorAndAbort(
              "Expected a return type of the form f[r], where f is a type constructor and r is a type."
            )

      // Error cases
      case DefDef(_, _, _, None) =>
        report.errorAndAbort(
          "@instrument has been applied to a definition without a defining term."
        )
      case _ =>
        report.errorAndAbort(
          "@instrument has been applied to an unsupported definition. Only method definitions are supported."
        )
    }
  }
}
