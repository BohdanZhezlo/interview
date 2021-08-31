package forex.programs.rates

import forex.services.rates.errors.{ Error => ServiceError }

object errors {

  sealed abstract class Error(val message: String)
  object Error {
    final case class RateNotFound(override val message: String) extends Error(message)
    final case class RatesGenericError(override val message: String) extends Error(message)
  }

  def toProgramError(error: ServiceError): Error = error match {
    case e: ServiceError.RateNotFound => Error.RateNotFound(e.message)
  }
}
