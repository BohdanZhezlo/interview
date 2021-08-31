package forex.http.rates

import cats.syntax.show._
import forex.programs.rates.errors.Error.{ RateNotFound, RatesGenericError }
import forex.programs.rates.errors.{ Error => RatesError }
import io.circe.Encoder
import org.http4s.Status

object errors {

  case class Error(httpStatus: Status, message: String)

  object Error {

    implicit class RatesErrorOps(private val ratesError: RatesError) extends AnyVal {
      def toApiError: Error =
        ratesError match {
          case RateNotFound(pair)         => Error(Status.NotFound, s"Could not find rate for ${pair.show} currency pair")
          case RatesGenericError(message) => Error(Status.InternalServerError, message)
        }
    }

    implicit val errorEncoder: Encoder[Error] =
      Encoder.forProduct1[Error, String]("message")(_.message)
  }
}
