package forex.http
package rates

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._
import forex.http.rates.errors.Error
import forex.http.rates.errors.Error._
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ EntityEncoder, HttpRoutes, Response }

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Right(rate) => Ok(rate.asGetApiResponse)
        case Left(error) => errorToResponse(error.toHttpError)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

  private def errorToResponse(error: Error)(implicit encoder: EntityEncoder[F, Error]): F[Response[F]] =
    Response(status = error.httpStatus, body = encoder.toEntity(error).body).pure[F]

}
