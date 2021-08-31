package forex.services.rates

import cats.syntax.show._
import forex.domain.Rate.Pair

object errors {

  sealed abstract class Error(val message: String)
  object Error {
    final case class RateNotFound(pair: Pair) extends Error(s"Could not find rate for ${pair.show} currency pair")
  }

}
