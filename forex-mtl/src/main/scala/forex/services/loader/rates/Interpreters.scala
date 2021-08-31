package forex.services.loader.rates

import cats.effect.{ Concurrent, Timer }
import forex.config.RatesLoaderConfig
import forex.services.loader.rates.interpreters.RatesLoader
import forex.services.{ RatesProvider, RatesStorage }

object Interpreters {

  def live[F[_]: Timer: Concurrent](provider: RatesProvider[F],
                                    storage: RatesStorage[F],
                                    config: RatesLoaderConfig): Algebra[F] =
    new RatesLoader[F](provider, storage, config)
}
