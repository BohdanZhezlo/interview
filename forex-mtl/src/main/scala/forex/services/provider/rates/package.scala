package forex.services.provider

import forex.domain.{ Currency, Price, Timestamp }
import forex.http._
import forex.services.provider.rates.Protocol.{ OneFrameRate, OneFrameRatesResponse }
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

package object rates {
  implicit val oneFrameRatesResponseDecoder: Decoder[OneFrameRatesResponse] = deriveDecoder

  implicit val oneFrameRateDecoder: Decoder[OneFrameRate] =
    Decoder.forProduct4[OneFrameRate, Currency, Currency, Price, Timestamp](
      "from",
      "to",
      "price",
      "time_stamp"
    )(OneFrameRate.apply)
}
