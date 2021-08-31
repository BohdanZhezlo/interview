package forex.domain

import cats.Show
import cats.syntax.show._
import forex.domain.Currency.AllCurrencies

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val show: Show[Pair] = Show.show(p => s"${p.from.show}${p.to.show}")

    val AllPairs: Seq[Pair] =
      AllCurrencies.combinations(2).flatMap { case Seq(c1, c2) => Seq(Pair(c1, c2), Pair(c2, c1)) }.toSeq
  }
}
