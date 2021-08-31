package forex.services.storage.rates.interpreters

import cats.effect.Sync
import cats.syntax.functor._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.storage.rates.Algebra

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

//TODO: scalacache as a storage?
class InMemoryRatesStorage[F[_]: Sync] private (storage: mutable.Map[Pair, Rate]) extends Algebra[F] {

  override def get(pair: Pair): F[Option[Rate]] =
    Sync[F].delay(storage.get(pair))

  override def put(pair: Pair, rate: Rate): F[Unit] =
    Sync[F].delay(storage += (pair -> rate)).void

  override def putAll(rates: Map[Pair, Rate]): F[Unit] =
    Sync[F].delay(storage ++= rates).void
}

object InMemoryRatesStorage {
  def apply[F[_]: Sync]: InMemoryRatesStorage[F] = new InMemoryRatesStorage[F](TrieMap.empty)
}
