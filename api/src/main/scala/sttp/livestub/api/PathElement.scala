package sttp.livestub.api

sealed trait PathElement {
  def matches(requestPath: RequestPath): Boolean
}

object PathElement {
  case class Fixed(path: String) extends PathElement {
    override def matches(requestPath: RequestPath): Boolean = requestPath.path == path
  }
  case object Wildcard extends PathElement {
    override def matches(requestPath: RequestPath): Boolean = true
  }
  case object MultiWildcard extends PathElement {
    override def matches(requestPath: RequestPath): Boolean = true
  }

  def fromString(strPath: String): PathElement = {
    strPath match {
      case "*"   => PathElement.Wildcard
      case "**"  => PathElement.MultiWildcard
      case other => PathElement.Fixed(other)
    }
  }
}
