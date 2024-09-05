/*
 * Copyright 2024 Ryosuke Kondo
 *
 * SPDX-License-Identifier: MIT
 */

package io.github.kory33.tracing_instrument.otel4s

import cats.Show.ContravariantShow
import io.github.kory33.tracing_instrument.core.macros.MacrosUtil
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.Tracer

import scala.compiletime.summonInline
import scala.quoted.*

object methodSpan {
  inline def apply[F[_], A](
      inline f: Span[F] => F[A]
  )(using t: Tracer[F]): F[A] = ${ applyImpl[F, A]('f, 't) }

  private[otel4s] def tryToPresentAsAttributeOfType[T: Type](using
      Quotes
  )(parameterDef: quotes.reflect.ValDef): Option[Expr[Attribute[T]]] = {
    import quotes.reflect.*

    val attributeNameExpr = Expr(parameterDef.name)
    for {
      ksExpr <- Expr.summon[AttributeKey.KeySelect[T]]
      expr <- Option.when(parameterDef.tpt.tpe <:< TypeRepr.of[T]) {
        '{
          Attribute(
            ${ attributeNameExpr },
            ${ Ref(parameterDef.symbol).asExprOf[T] }
          )(using ${ ksExpr })
        }
      }
    } yield expr
  }

  private[otel4s] def tryToPresentAsStringAttributeViaShow(using
      Quotes
  )(parameterDef: quotes.reflect.ValDef): Option[Expr[Attribute[String]]] = {
    import quotes.reflect.*

    parameterDef.tpt.tpe.asType match {
      case '[t] =>
        Expr.summon[ContravariantShow[t]].map { instanceExpr =>
          '{
            Attribute[String](
              ${ Expr(parameterDef.name) },
              ${ instanceExpr }.show(${ Ref(parameterDef.symbol).asExprOf[t] })
            )
          }
        }
    }
  }

  def applyImpl[F[_]: Type, A: Type](using
      Quotes
  )(f: Expr[Span[F] => F[A]], t: Expr[Tracer[F]]): Expr[F[A]] = {
    import quotes.reflect.*

    val enclosingDefDef = MacrosUtil.enclosingDefDef.getOrElse {
      report.errorAndAbort(
        "Failed to find the enclosing method definition."
      )
    }

    val spanName = enclosingDefDef.name
    val attributesExpr: Expr[Seq[Attribute[?]]] = {
      Expr.ofSeq {
        MacrosUtil.defDefValParameters(enclosingDefDef).flatMap { valDef =>
          tryToPresentAsAttributeOfType[String](valDef)
            .orElse(tryToPresentAsAttributeOfType[Boolean](valDef))
            .orElse(tryToPresentAsAttributeOfType[Long](valDef))
            .orElse(tryToPresentAsAttributeOfType[Double](valDef))
            .orElse(tryToPresentAsAttributeOfType[Seq[String]](valDef))
            .orElse(tryToPresentAsAttributeOfType[Seq[Boolean]](valDef))
            .orElse(tryToPresentAsAttributeOfType[Seq[Long]](valDef))
            .orElse(tryToPresentAsAttributeOfType[Seq[Double]](valDef))
            .orElse(tryToPresentAsStringAttributeViaShow(valDef))
        }
      }
    }

    '{
      ${ t }
        .span(
          name = ${ Expr(spanName) },
          attributes = ${ attributesExpr }*
        )
        .use($f)
    }
  }
}
