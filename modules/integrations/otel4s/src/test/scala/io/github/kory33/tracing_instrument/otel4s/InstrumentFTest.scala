/*
 * Copyright 2024 Ryosuke Kondo
 *
 * SPDX-License-Identifier: MIT
 */

package io.github.kory33.tracing_instrument.otel4s

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.Inside
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.sdk.testkit.trace.TracesTestkit
import org.typelevel.otel4s.trace.Tracer

import scala.concurrent.duration.*

class InstrumentFTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside {
  "instrumentF" - {
    @instrumentF
    def sleepWithTracing(using Tracer[IO]): IO[Unit] =
      IO.sleep(40.millis)

    "records a span with the path of the enclosing method" in {
      TracesTestkit.inMemory[IO]().use { testkit =>
        for {
          given Tracer[IO] <- testkit.tracerProvider.get("test")
          _ <- sleepWithTracing
          spans <- testkit.finishedSpans
        } yield {
          inside(spans) { case List(span) =>
            span.name mustBe "sleepWithTracing"
          }
        }
      }
    }

    @instrumentF
    def nestedSleepWithTracing(using Tracer[IO]): IO[Unit] =
      sleepWithTracing >> sleepWithTracing

    "captures nested spans" in {
      TracesTestkit.inMemory[IO]().use { testkit =>
        for {
          given Tracer[IO] <- testkit.tracerProvider.get("test")
          _ <- nestedSleepWithTracing
          spans <- testkit.finishedSpans
        } yield {
          /* Span tree should look like:
           * nestedSleepWithTracing (third span to be completed)
           *  |- sleepWithTracing (first span to be completed)
           *  |- sleepWithTracing (second span to be completed)
           */
          inside(spans) { case List(firstSpan, secondSpan, thirdSpan) =>
            inside(firstSpan) { case span =>
              span.parentSpanContext.map(_.spanId) mustBe Some(
                thirdSpan.spanContext.spanId
              )
              span.name mustBe "sleepWithTracing"
            }

            inside(secondSpan) { case span =>
              span.parentSpanContext.map(_.spanId) mustBe Some(
                thirdSpan.spanContext.spanId
              )
              span.name mustBe "sleepWithTracing"
            }

            inside(thirdSpan) { case span =>
              span.parentSpanContext mustBe empty
              span.name mustBe "nestedSleepWithTracing"
            }
          }
        }
      }
    }

    @instrumentF
    def emptyFunctionWithParams(a: Int, b: String)(using Tracer[IO]): IO[Unit] =
      IO.unit

    "records attributes" in {
      TracesTestkit.inMemory[IO]().use { testkit =>
        for {
          given Tracer[IO] <- testkit.tracerProvider.get("test")
          _ <- emptyFunctionWithParams(42, "hello")
          spans <- testkit.finishedSpans
        } yield {
          inside(spans) { case List(span) =>
            println(span)
            span.attributes.elements must contain allOf (
              // TODO: with a more sophisticated implementation, we can have 42 as an integer attribute
              //       instead of a string attribute
              Attribute("a", "42"),
              Attribute("b", "hello")
            )
          }
        }
      }
    }
  }
}
