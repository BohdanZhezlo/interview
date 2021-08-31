package forex.services.rates

import cats.{ Applicative, Functor }
import forex.services.ReadOnlyRatesStorage
import forex.services.rates.interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F]                              = new OneFrameDummy[F]()
  def live[F[_]: Functor](storage: ReadOnlyRatesStorage[F]): Algebra[F] = new PersistentRatesService[F](storage)
}
