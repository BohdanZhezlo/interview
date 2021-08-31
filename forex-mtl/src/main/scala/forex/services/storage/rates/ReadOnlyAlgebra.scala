package forex.services.storage.rates

import forex.domain.Rate
import forex.domain.Rate.Pair

trait ReadOnlyAlgebra[F[_]] {
  def get(pair: Pair): F[Option[Rate]]
}
