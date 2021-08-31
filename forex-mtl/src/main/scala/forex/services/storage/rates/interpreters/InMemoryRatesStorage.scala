package forex.services.storage.rates.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.functor._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.storage.rates.Algebra

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

//TODO: scalacache as a storage?
class InMemoryRatesStorage[F[_]: Applicative] private (storage: mutable.Map[Pair, Rate]) extends Algebra[F] {

  override def get(pair: Pair): F[Option[Rate]] =
    storage.get(pair).pure[F]

  override def put(pair: Pair, rate: Rate): F[Unit] =
    (storage += (pair -> rate)).pure[F].void

  override def putAll(rates: Map[Pair, Rate]): F[Unit] =
    (storage ++= rates).pure[F].void
}

object InMemoryRatesStorage {
  def apply[F[_]: Applicative]: InMemoryRatesStorage[F] = new InMemoryRatesStorage[F](TrieMap.empty)
}
