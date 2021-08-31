package forex.services.loader.rates

import cats.data.NonEmptySeq
import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.applicative._
import cats.syntax.either._
import forex.config.RatesLoaderConfig
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.loader.rates.interpreters.RatesLoader
import forex.services.provider.rates.errors.{ Error => ProviderError }
import forex.services.{ RatesProvider, RatesStorage }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class RatesLoaderSpec extends AnyWordSpec with Matchers {

  private implicit val timer: Timer[IO]               = IO.timer(ExecutionContext.global)
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val rates = Seq(
    Rate(Pair(EUR, USD), Price(1.0), Timestamp.now),
    Rate(Pair(CAD, JPY), Price(2.0), Timestamp.now)
  )

  "a RatesLoader" when {
    "when loading rates" should {
      "should store fetched rates and handle errors" in {
        val storedRates: mutable.Map[Pair, Rate] = mutable.Map.empty
        var fetchRatesCalledTimes: Int           = 0

        val ratesStorage = new RatesStorage[IO] {
          override def get(pair: Pair): IO[Option[Rate]]     = None.pure[IO]
          override def put(pair: Pair, rate: Rate): IO[Unit] = ().pure[IO]
          override def putAll(rates: Map[Pair, Rate]): IO[Unit] = {
            storedRates ++= rates
            ().pure[IO]
          }
        }

        val ratesProvider = new RatesProvider[IO] {
          override def fetchRates(pairs: NonEmptySeq[Pair]): IO[ProviderError Either Seq[Rate]] = {
            val result =
              if (fetchRatesCalledTimes == 0) ProviderError.FetchRatesFailed("BOOM!").asLeft[Seq[Rate]]
              else rates.asRight[ProviderError]
            fetchRatesCalledTimes += 1
            result.pure[IO]
          }
        }

        new RatesLoader[IO](ratesProvider, ratesStorage, RatesLoaderConfig(1.second)).load.compile.drain.unsafeRunSync()

        fetchRatesCalledTimes shouldBe 2
        storedRates.values should contain theSameElementsAs rates
      }
    }
  }
}
