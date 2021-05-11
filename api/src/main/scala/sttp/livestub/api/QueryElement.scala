package sttp.livestub.api

sealed trait QueryElement {
  def isRequired: Boolean
}

object QueryElement {
  case class FixedQuery(key: String, values: Seq[String], isRequired: Boolean) extends QueryElement
  case class WildcardValueQuery(key: String, isRequired: Boolean) extends QueryElement
  case object WildcardQuery extends QueryElement {
    override def isRequired: Boolean = false
  }
}
