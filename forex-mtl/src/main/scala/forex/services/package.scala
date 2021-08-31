package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type RatesProvider[F[_]] = provider.rates.Algebra[F]
  final val RatesProviders = provider.rates.Interpreters

  type RatesLoader[F[_]] = loader.rates.Algebra[F]
  final val RatesLoader = loader.rates.Interpreters

  type RatesStorage[F[_]]         = storage.rates.Algebra[F]
  type ReadOnlyRatesStorage[F[_]] = storage.rates.ReadOnlyAlgebra[F]
  final val RatesStorages = storage.rates.Interpreters
}
