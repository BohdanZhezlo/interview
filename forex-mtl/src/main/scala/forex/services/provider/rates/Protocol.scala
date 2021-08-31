package forex.services.provider.rates

import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }

object Protocol {

  final case class OneFrameRate(from: Currency, to: Currency, price: Price, timestamp: Timestamp) {
    def toRate: Rate = Rate(pair = Pair(from = from, to = to), price = price, timestamp = timestamp)
  }

  final case class OneFrameRatesResponse(rates: Seq[OneFrameRate])

}
