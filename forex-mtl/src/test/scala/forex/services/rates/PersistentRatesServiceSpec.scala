package forex.services.rates

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import forex.domain.Currency.{ EUR, USD }
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.ReadOnlyRatesStorage
import forex.services.rates.errors.Error.RateNotFound
import forex.services.rates.errors._
import forex.services.rates.interpreters.PersistentRatesService
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{ Instant, OffsetDateTime, ZoneOffset }

class PersistentRatesServiceSpec extends AnyWordSpec with Matchers {

  private val pairToFetch = Pair(USD, EUR)

  "a PersistentRatesService" when {
    "getting rates" should {
      "successfully return rate" in {
        val rate = Rate(
          Pair(EUR, USD),
          Price(7.123),
          Timestamp(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
        )
        performTest(rate.some, rate.asRight)
      }

      "return NotFound when no rate is present" in {
        performTest(None, RateNotFound(pairToFetch).asLeft)
      }
    }
  }

  private def performTest(storageResult: Option[Rate], expectedServiceResult: Error Either Rate): Assertion = {
    val ratesStorage = new ReadOnlyRatesStorage[IO] {
      override def get(pair: Pair): IO[Option[Rate]] = {
        pair shouldBe pairToFetch
        storageResult.pure[IO]
      }
    }
    val response = new PersistentRatesService[IO](ratesStorage).get(pairToFetch).unsafeRunSync()
    response shouldBe expectedServiceResult
  }
}
