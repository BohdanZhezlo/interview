package forex.http.rates

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.RateNotFound
import forex.programs.rates.errors._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{ Instant, OffsetDateTime, ZoneOffset }

class RatesHttpRoutesSpec extends AnyWordSpec with Matchers {

  private val pair = Pair(EUR, USD)

  "a RatesHttpRoutes" when {
    "handling incoming GET requests on /rates path" should {
      "return successful response" in {
        val rateResult = Rate(
          pair,
          Price(7.123),
          Timestamp(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
        ).asRight[Error]
        val expectedBody = Json
          .obj(
            ("from", Json.fromString("EUR")),
            ("to", Json.fromString("USD")),
            ("price", Json.fromBigDecimal(7.123)),
            ("timestamp", Json.fromString("1970-01-01T00:00:00Z"))
          )
        runTest(rateResult, Status.Ok, expectedBody)
      }

      "return error response" in {
        val rateResult   = RateNotFound(pair).asLeft[Rate]
        val expectedBody = Json.obj(("message", Json.fromString("Could not find rate for EURUSD currency pair")))
        runTest(rateResult, Status.NotFound, expectedBody)
      }
    }
  }

  private def runTest(rateResult: Error Either Rate, expectedStatus: Status, expectedBody: Json): Assertion = {
    val actualResponse = new RatesHttpRoutes((_: GetRatesRequest) => rateResult.pure[IO]).routes.orNotFound
      .run(Request[IO](uri = uri"/rates?from=EUR&to=USD"))
      .unsafeRunSync()

    actualResponse.status shouldBe expectedStatus
    actualResponse.as[Json].unsafeRunSync() shouldBe expectedBody
  }

}
