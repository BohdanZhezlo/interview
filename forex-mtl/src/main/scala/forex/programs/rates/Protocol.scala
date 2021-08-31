package forex.programs.rates

import forex.domain.Currency
import forex.domain.Rate.Pair

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  ) {
    lazy val pair: Pair = Pair(from, to)
  }

}
