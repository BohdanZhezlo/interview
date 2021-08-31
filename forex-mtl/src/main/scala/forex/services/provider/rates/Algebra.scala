package forex.services.provider.rates

import cats.data.NonEmptySeq
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.provider.rates.errors._

trait Algebra[F[_]] {
  def fetchRates(pairs: NonEmptySeq[Pair]): F[Error Either Seq[Rate]]
}
