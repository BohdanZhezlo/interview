package forex.programs.rates

import forex.domain.Rate.Pair
import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error
  object Error {
    final case class RateNotFound(pair: Pair) extends Error
    final case class RatesGenericError(message: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.RateNotFound(pair) => Error.RateNotFound(pair)
  }
}
