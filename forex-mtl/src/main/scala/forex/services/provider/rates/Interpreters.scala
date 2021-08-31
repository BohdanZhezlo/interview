package forex.services.provider.rates

import cats.effect.Sync
import forex.config.OneFrameRatesProviderConfig
import forex.services.provider.rates.interpreters.OneFrameRatesProvider
import org.http4s.client.Client

object Interpreters {

  def oneFrame[F[_]: Sync](config: OneFrameRatesProviderConfig, httpClient: Client[F]): Algebra[F] =
    new OneFrameRatesProvider[F](config, httpClient)
}
