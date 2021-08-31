package forex.services.rates

import forex.domain.Rate.Pair

object errors {

  sealed trait Error
  object Error {
    final case class RateNotFound(pair: Pair) extends Error
  }

}
