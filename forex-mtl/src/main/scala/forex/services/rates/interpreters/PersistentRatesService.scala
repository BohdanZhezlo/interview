package forex.services.rates.interpreters

import cats.Functor
import cats.syntax.functor._
import forex.domain.Rate
import forex.services.ReadOnlyRatesStorage
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.RateNotFound
import forex.services.rates.errors._

class PersistentRatesService[F[_]: Functor](storage: ReadOnlyRatesStorage[F]) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    storage.get(pair).map(_.toRight(RateNotFound(pair)))
}
