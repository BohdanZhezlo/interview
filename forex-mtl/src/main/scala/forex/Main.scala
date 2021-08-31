package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      httpClient <- oneFrameHttpClient(ec, config.oneFrameRatesProvider)
      module = new Module[F](config, httpClient)
      _ <- module.ratesLoader.load
      _ <- httpServer(ec, config, module)
    } yield ()

  private def httpServer(ec: ExecutionContext, config: ApplicationConfig, module: Module[F]): Stream[F, ExitCode] =
    BlazeServerBuilder[F](ec)
      .bindHttp(config.http.port, config.http.host)
      .withHttpApp(module.httpApp)
      .serve

  private def oneFrameHttpClient(ec: ExecutionContext, config: OneFrameRatesProviderConfig): Stream[F, Client[F]] = {
    val retryPolicy =
      RetryPolicy[F](RetryPolicy.exponentialBackoff(maxWait = config.retryDuration, maxRetry = config.maxRetries))
    BlazeClientBuilder[F](ec)
      .withRequestTimeout(config.http.timeout)
      .stream
      .map(client => Retry[F](retryPolicy)(client))
  }

}
