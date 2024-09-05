package io.github.kory33.tracing_instrument.otel4s

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.must.Matchers
import org.typelevel.otel4s.trace.Tracer
import cats.effect.IO
import scala.concurrent.duration.*
import org.typelevel.otel4s.sdk.testkit.trace.TracesTestkit
import org.scalatest.Inside

class InstrumentFTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside {
  "instrumentF" - {
    val sleepWithTracingQualifiedMethodName =
      "io.github.kory33.tracing_instrument.otel4s.InstrumentFTest#sleepWithTracing"
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
            span.name mustBe sleepWithTracingQualifiedMethodName
          }
        }
      }
    }

    val nestedSleepWithTracingQualifiedMethodName =
      "io.github.kory33.tracing_instrument.otel4s.InstrumentFTest#nestedSleepWithTracing"
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
            firstSpan.parentSpanContext.get.spanId mustBe thirdSpan.spanContext.spanId
            firstSpan.name mustBe sleepWithTracingQualifiedMethodName

            secondSpan.parentSpanContext.get.spanId mustBe thirdSpan.spanContext.spanId
            secondSpan.name mustBe sleepWithTracingQualifiedMethodName

            thirdSpan.parentSpanContext mustBe empty
            thirdSpan.name mustBe nestedSleepWithTracingQualifiedMethodName
          }
        }
      }
    }
  }
}
