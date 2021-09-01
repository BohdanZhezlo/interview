package forex.http.rates

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.show._
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
        runTest(pair.from.show.toLowerCase, pair.to.show, rateResult, Status.Ok, expectedBody)
      }

      "return error response" in {
        val rateResult   = RateNotFound(pair).asLeft[Rate]
        val expectedBody = Json.obj(("message", Json.fromString("Could not find rate for EURUSD currency pair")))
        runTest(pair.from.show, pair.to.show, rateResult, Status.NotFound, expectedBody)
      }

      "handle invalid query parameters" in {
        val rateResult = RateNotFound(pair).asLeft[Rate]
        def expectedBody(paramNames: Seq[String]): Json =
          Json.obj(
            (
              "message",
              Json.fromString(
                paramNames.map(name => s"Query parameter '$name' currency is not supported").mkString(", ")
              )
            )
          )

        runTest("UAH", pair.to.show, rateResult, Status.BadRequest, expectedBody(Seq("from")))
        runTest(pair.from.show, "RUB", rateResult, Status.BadRequest, expectedBody(Seq("to")))
        runTest("PLN", "CZK", rateResult, Status.BadRequest, expectedBody(Seq("from", "to")))

        val expectedBodySameCurrencies =
          Json.obj(("message", Json.fromString("Query parameters 'from' and 'to' must be different currencies")))
        runTest(pair.to.show, pair.to.show, rateResult, Status.BadRequest, expectedBodySameCurrencies)
      }
    }
  }

  private def runTest(from: String,
                      to: String,
                      rateResult: Error Either Rate,
                      expectedStatus: Status,
                      expectedBody: Json): Assertion = {
    val actualResponse = new RatesHttpRoutes((_: GetRatesRequest) => rateResult.pure[IO]).routes.orNotFound
      .run(Request[IO](uri = Uri.unsafeFromString(s"/rates?from=$from&to=$to")))
      .unsafeRunSync()

    actualResponse.status shouldBe expectedStatus
    actualResponse.as[Json].unsafeRunSync() shouldBe expectedBody
  }

}
