package forex.services.storage.rates

import cats.Applicative
import forex.services.storage.rates.interpreters.InMemoryRatesStorage

object Interpreters {
  def inMemory[F[_]: Applicative]: Algebra[F] = InMemoryRatesStorage[F]
}
