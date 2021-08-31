package forex.services.provider.rates

import scala.util.control.NoStackTrace

object errors {

  sealed abstract class Error(val message: String) extends Throwable(message) with NoStackTrace
  object Error {
    final case class UriNotParseable(override val message: String) extends Error(message)
    final case class FetchRatesFailed(override val message: String) extends Error(message)
    final case class EmptyFetchRatesRequest(override val message: String) extends Error(message)
  }
}
