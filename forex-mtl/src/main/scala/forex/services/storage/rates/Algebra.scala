package forex.services.storage.rates

import forex.domain.Rate
import forex.domain.Rate.Pair

trait Algebra[F[_]] extends ReadOnlyAlgebra[F] {
  def put(pair: Pair, rate: Rate): F[Unit]
  def putAll(rates: Map[Pair, Rate]): F[Unit]
}
