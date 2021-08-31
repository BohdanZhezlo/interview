package forex.http.rates

import forex.programs.rates.errors.Error.RateNotFound
import forex.programs.rates.errors.{ Error => RatesError }
import io.circe.Encoder
import org.http4s.Status

object errors {

  case class Error(httpStatus: Status, message: String)

  object Error {

    implicit class RatesErrorOps(private val ratesError: RatesError) extends AnyVal {
      def toHttpError: Error =
        ratesError match {
          case RateNotFound(message) => Error(Status.NotFound, message)
          case other                 => Error(Status.InternalServerError, other.message)
        }
    }

    implicit val errorEncoder: Encoder[Error] =
      Encoder.forProduct1[Error, String]("message")(_.message)
  }
}
