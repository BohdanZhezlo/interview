package forex.services.provider.rates

object errors {

  sealed trait Error
  object Error {
    final case class UriNotParseable(message: String) extends Error
    final case class FetchRatesFailed(message: String) extends Error
  }
}
