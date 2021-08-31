package forex.services.storage.rates

import cats.effect.IO
import cats.syntax.option._
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.storage.rates.interpreters.InMemoryRatesStorage
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InMemoryRatesStorageSpec extends AnyWordSpec with Matchers {

  private val now                   = Timestamp.now
  private val (pair1, pair2, pair3) = (Pair(USD, EUR), Pair(CAD, JPY), Pair(CHF, NZD))
  private val (rate1, rate2)        = (Rate(pair1, Price(1.1), now), Rate(pair2, Price(2.2), now))

  "an InMemoryRatesStorage" when {
    "when storing a rate" should {
      "return stored rate" in {
        val storage = InMemoryRatesStorage[IO]

        storage.put(pair1, rate1).unsafeRunSync()
        storage.put(pair2, rate2).unsafeRunSync()

        storage.get(pair1).unsafeRunSync() shouldBe rate1.some
        storage.get(pair2).unsafeRunSync() shouldBe rate2.some
        storage.get(pair3).unsafeRunSync() shouldBe None
      }
    }
    "when storing all rates" should {
      "return stored rates" in {
        val storage = InMemoryRatesStorage[IO]

        storage.putAll(Map((pair1, rate1), (pair2, rate2))).unsafeRunSync()

        storage.get(pair1).unsafeRunSync() shouldBe rate1.some
        storage.get(pair2).unsafeRunSync() shouldBe rate2.some
        storage.get(pair3).unsafeRunSync() shouldBe None
      }
    }
  }
}
