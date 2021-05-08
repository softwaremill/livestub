package sttp.livestub.api

sealed trait QueryElement {
  def matches(requestQuery: RequestQuery): Boolean
  def isRequired: Boolean
}

object QueryElement {
  case class FixedQuery(key: String, values: Seq[String], isRequired: Boolean) extends QueryElement {
    override def matches(requestQuery: RequestQuery): Boolean =
      requestQuery.key == key && requestQuery.values.iterator.sameElements(values)
  }
  case class WildcardValueQuery(key: String, isRequired: Boolean) extends QueryElement {
    override def matches(requestQuery: RequestQuery): Boolean = requestQuery.key == key
  }
  case object WildcardQuery extends QueryElement {
    override def matches(requestQuery: RequestQuery): Boolean = true

    override def isRequired: Boolean = false
  }
}
