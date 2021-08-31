package forex.services.provider.rates

import cats.data.NonEmptySeq
import cats.effect.{ IO, Resource }
import cats.syntax.option._
import cats.syntax.show._
import forex.config.{ HttpConfig, OneFrameRatesProviderConfig }
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.provider.rates.errors.Error
import forex.services.provider.rates.errors.Error.{ FetchRatesFailed, UriNotParseable }
import forex.services.provider.rates.interpreters.OneFrameRatesProvider
import fs2.Stream
import io.circe.Json
import org.http4s.client.Client
import org.http4s.{ Header, Method, Request, Response, Status }
import org.scalatest.Assertions.fail
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ Assertion, EitherValues }

import java.time.OffsetDateTime
import scala.concurrent.duration._

class OneFrameRatesProviderSpec extends AnyWordSpec with Matchers with OneFrameRatesProviderSupport {

  private val timestamp = "2021-08-31T11:22:33.777Z"

  private val pairs = NonEmptySeq.fromSeqUnsafe(
    Seq(
      Pair(USD, EUR),
      Pair(AUD, GBP),
      Pair(JPY, CAD)
    )
  )

  private val expectedRates = pairs.zipWithIndex.map {
    case (pair, idx) =>
      Rate(
        pair = pair,
        price = Price(BigDecimal(idx) + 0.13),
        timestamp = Timestamp(OffsetDateTime.parse(timestamp))
      )
  }

  private val validResponse = {
    val jsonPairs = expectedRates.map { rate =>
      Json.obj(
        ("from", Json.fromString(rate.pair.from.show)),
        ("to", Json.fromString(rate.pair.to.show)),
        ("bid", Json.fromBigDecimal(rate.price.value + 1)),
        ("ask", Json.fromBigDecimal(rate.price.value + 2)),
        ("price", Json.fromBigDecimal(rate.price.value)),
        ("time_stamp", Json.fromString(timestamp)),
      )
    }

    Json.arr(jsonPairs.toSeq: _*)
  }

  "a OneFrameRatesProvider" when {
    "fetching rates" should {
      "return all rates" in {
        havingOneFrameRateProvider
          .withHttpResponse(Status.Ok, validResponse)
          .whenFetchingRates(pairs)
          .verifySuccessfulResponse { response =>
            response should contain theSameElementsAs expectedRates.toSeq
          }
          .verifyRequest { request =>
            request.method shouldBe Method.GET
            request.uri.show shouldBe "http://localhost:80/rates?pair=USDEUR&pair=AUDGBP&pair=JPYCAD"
            request.headers.toList should contain(Header("token", "aToken"))
          }
      }

      "return empty result" in {
        havingOneFrameRateProvider
          .withHttpResponse(Status.Ok, Json.arr())
          .whenFetchingRates(pairs)
          .verifySuccessfulResponse { response =>
            response shouldBe empty
          }
      }

      "handle URI construction failure" in {
        havingOneFrameRateProviderWithInvalidHttpConfig
          .whenFetchingRates(pairs)
          .verifyFailedResponse { error =>
            error shouldBe a[UriNotParseable]
          }
      }

      "handle fetch failure" in {
        havingOneFrameRateProvider
          .withHttpResponse(Status.BadRequest, Json.Null)
          .whenFetchingRates(pairs)
          .verifyFailedResponse { error =>
            error shouldBe a[FetchRatesFailed]
          }
      }

      "handle invalid response format" in {
        havingOneFrameRateProvider
          .withHttpResponse(Status.Ok, Json.fromString("wat?"))
          .whenFetchingRates(pairs)
          .verifyFailedResponse { error =>
            error shouldBe a[FetchRatesFailed]
          }
      }
    }
  }
}

trait OneFrameRatesProviderSupport {

  import OneFrameRatesProviderSupport._

  def havingOneFrameRateProvider: OneFrameRatesProviderForTest =
    constructProvider("localhost")

  def havingOneFrameRateProviderWithInvalidHttpConfig: OneFrameRatesProviderForTest =
    constructProvider("[]")

  private def constructProvider(host: String): OneFrameRatesProviderForTest = {
    val config = OneFrameRatesProviderConfig(
      http = HttpConfig(host = host, port = 80, timeout = 1.second),
      authToken = "aToken",
      maxRetries = 1,
      retryDuration = 1.second
    )
    val httpClientState = new HttpClientState(None, None)

    new OneFrameRatesProviderForTest(config, httpClientState)
  }
}

object OneFrameRatesProviderSupport {

  class OneFrameRatesProviderForTest(config: OneFrameRatesProviderConfig, val httpClientState: HttpClientState)
      extends OneFrameRatesProvider[IO](config, httpClientState.client)

  implicit class OneFrameRatesProviderForTestState(private val provider: OneFrameRatesProviderForTest) extends AnyVal {

    def withHttpResponse(status: Status, response: Json): OneFrameRatesProviderForTestState = {
      provider.httpClientState.response = ResponseWrapper(status, response).some
      this
    }

    def whenFetchingRates(pairs: NonEmptySeq[Pair]): CapturedResultVerification =
      new CapturedResultVerification(
        provider.fetchRates(pairs).unsafeRunSync(),
        provider.httpClientState.request
      )
  }

  class CapturedResultVerification(capturedResult: Error Either Seq[Rate], capturedRequest: Option[Request[IO]])
      extends Matchers
      with EitherValues {

    def verifySuccessfulResponse(verification: Seq[Rate] => Assertion): CapturedResultVerification = {
      verification(capturedResult.value)
      this
    }

    def verifyFailedResponse(verification: Error => Assertion): CapturedResultVerification = {
      verification(capturedResult.left.value)
      this
    }

    def verifyRequest(verification: Request[IO] => Assertion): CapturedResultVerification = {
      capturedRequest.fold[Assertion](
        fail("Request hasn't been captured. Please call 'whenFetchingRates' before verifying request")
      )(verification)
      this
    }
  }

  class HttpClientState(var response: Option[ResponseWrapper], var request: Option[Request[IO]]) {
    val client: Client[IO] = Client[IO] { request =>
      this.request = request.some
      getResponse
    }

    private def getResponse: Resource[IO, Response[IO]] =
      response.fold[Resource[IO, Response[IO]]](
        fail("Response hasn't been set. Please call 'withHttpResponse' before verifying response")
      ) { response =>
        Resource.pure[IO, Response[IO]](
          Response(
            status = response.status,
            body = Stream.emits(response.body.spaces2.getBytes("UTF-8"))
          )
        )
      }
  }

  case class ResponseWrapper(status: Status, body: Json)

}
