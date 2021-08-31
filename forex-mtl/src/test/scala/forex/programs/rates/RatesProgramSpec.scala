package forex.programs.rates

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.{ Error => ProgramError }
import forex.services.RatesService
import forex.services.rates.errors.{ Error => ServiceError }
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{ Instant, OffsetDateTime, ZoneOffset }

class RatesProgramSpec extends AnyWordSpec with Matchers {

  private val rate = Rate(
    Pair(EUR, USD),
    Price(7.123),
    Timestamp(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
  )

  private val request = GetRatesRequest(EUR, USD)

  "a RatesProgram" when {
    "fetching a rate" should {
      "return successful result" in {
        performTest(rate.asRight[ServiceError], rate.asRight[ProgramError])
      }
      "handle error" in {
        performTest(
          ServiceError.RateNotFound(request.pair).asLeft,
          ProgramError.RateNotFound(request.pair).asLeft
        )
      }
    }
  }

  private def performTest(serviceResult: ServiceError Either Rate,
                          expectedProgramResult: ProgramError Either Rate): Assertion = {
    val ratesService = new RatesService[IO] {
      override def get(pair: Pair): IO[ServiceError Either Rate] = {
        pair shouldBe request.pair
        serviceResult.pure[IO]
      }
    }
    val response = Program[IO](ratesService).get(request).unsafeRunSync()
    response shouldBe expectedProgramResult
  }
}
