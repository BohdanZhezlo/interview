package forex.services.loader.rates

import fs2.Stream

trait Algebra[F[_]] {

  def load: Stream[F, Unit]
}
