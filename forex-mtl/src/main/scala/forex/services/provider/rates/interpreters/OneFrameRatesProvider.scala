package forex.services.provider.rates.interpreters

import cats.data.{ EitherT, NonEmptySeq }
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.show._
import forex.config.OneFrameRatesProviderConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http._
import forex.services.provider.rates.Algebra
import forex.services.provider.rates.Protocol.OneFrameRatesResponse
import forex.services.provider.rates.errors.Error.{ FetchRatesFailed, UriNotParseable }
import forex.services.provider.rates.errors._
import org.http4s.QueryParamEncoder._
import org.http4s.client.Client
import org.http4s.{ Header, Headers, Method, Request, Uri }

class OneFrameRatesProvider[F[_]: Sync](config: OneFrameRatesProviderConfig, httpClient: Client[F]) extends Algebra[F] {

  private val BaseUri = s"http://${config.http.host}:${config.http.port}/rates"

  override def fetchRates(pairs: NonEmptySeq[Pair]): F[Error Either Seq[Rate]] =
    (for {
      uri <- EitherT(constructUri(pairs))
      request = Request[F](
        method = Method.GET,
        uri = uri,
        headers = Headers.of(Header("token", config.authToken))
      )
      response <- EitherT(executeRequest(request))
    } yield response.rates.map(_.toRate)).value

  private def constructUri(pairs: NonEmptySeq[Pair]): F[Error Either Uri] =
    Uri
      .fromString(BaseUri)
      .bimap[Error, Uri](
        e => UriNotParseable(e.message),
        uri => uri.setQueryParams(Map("pair" -> pairs.map(_.show).toSeq))
      )
      .pure

  private def executeRequest(request: Request[F]): F[Error Either OneFrameRatesResponse] =
    httpClient
      .expect[OneFrameRatesResponse](request)
      .map(_.asRight[Error])
      .handleError(e => FetchRatesFailed(e.getMessage).asLeft)
}
