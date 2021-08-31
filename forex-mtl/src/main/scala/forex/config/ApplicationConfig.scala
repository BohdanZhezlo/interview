package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrameRatesProvider: OneFrameRatesProviderConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameRatesProviderConfig(
    http: HttpConfig,
    authToken: String,
    maxRetries: Int,
    retryDuration: FiniteDuration
)
