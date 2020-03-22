package sttp.livestub.api

sealed trait QueryElement

object QueryElement {
  case class FixedQuery(key: String, values: Seq[String]) extends QueryElement
  case class WildcardValueQuery(key: String) extends QueryElement
  case object WildcardQuery extends QueryElement
}
