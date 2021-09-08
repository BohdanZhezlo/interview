package forex.services.loader.rates.interpreters

import cats.ApplicativeError
import cats.data.NonEmptySeq
import cats.effect.{ Concurrent, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.config.RatesLoaderConfig
import forex.domain.Rate.Pair
import forex.services.loader.rates.Algebra
import forex.services.{ RatesProvider, RatesStorage }
import fs2.Stream

class RatesLoader[F[_]: Timer: Concurrent](provider: RatesProvider[F],
                                           storage: RatesStorage[F],
                                           config: RatesLoaderConfig)
    extends Algebra[F] {

  override def load: Stream[F, Unit] =
    Stream
      .eval(refreshRates())
      .concurrently(Stream.awakeEvery[F](config.reloadInterval).evalMap(_ => refreshRates()))
      .handleErrorWith(_ => load)

  private def refreshRates(): F[Unit] =
    for {
      rates <- provider
                .fetchRates(NonEmptySeq.fromSeqUnsafe(Pair.AllPairs))
                .flatMap(ApplicativeError[F, Throwable].fromEither)
      pairs = rates.map(r => r.pair -> r).toMap
      _ <- storage.putAll(pairs)
    } yield ()
}
