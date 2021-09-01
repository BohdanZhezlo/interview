package forex.http.rates

import cats.data.{ Validated, ValidatedNel }
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.validated._
import forex.domain.Currency
import forex.http.rates.errors.Error
import forex.http.rates.errors.Error._
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ EntityEncoder, HttpRoutes, Response, Status }

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      validateQueryParams(from, to).fold(
        toResponse,
        request => {
          rates.get(request).flatMap {
            case Right(rate) => Ok(rate.asGetApiResponse)
            case Left(error) => toResponse(error.toApiError)
          }
        }
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

  private def toResponse(error: Error)(implicit encoder: EntityEncoder[F, Error]): F[Response[F]] =
    Response(status = error.httpStatus, body = encoder.toEntity(error).body).pure[F]

  private def validateQueryParams(from: Option[Currency], to: Option[Currency]): Validated[Error, GetRatesRequest] =
    (
      validateCurrencyQueryParam(from, "from"),
      validateCurrencyQueryParam(to, "to")
    ).mapN(GetRatesRequest.apply)
      .leftMap(errors => Error(Status.BadRequest, errors.toList.mkString(", ")))
      .ensure(Error(Status.BadRequest, "Query parameters 'from' and 'to' must be different currencies"))(
        r => r.from != r.to
      )

  private def validateCurrencyQueryParam(value: Option[Currency], paramName: String): ValidatedNel[String, Currency] =
    value.fold(s"Query parameter '$paramName' currency is not supported".invalidNel[Currency])(_.validNel)
}
