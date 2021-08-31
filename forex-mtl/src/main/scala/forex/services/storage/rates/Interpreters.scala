package forex.services.storage.rates

import cats.effect.Sync
import interpreters.InMemoryRatesStorage

object Interpreters {
  def inMemory[F[_]: Sync]: Algebra[F] = InMemoryRatesStorage[F]
}
