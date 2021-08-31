package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type RatesProvider[F[_]] = provider.rates.Algebra[F]
  final val RatesProviders = provider.rates.Interpreters
}
