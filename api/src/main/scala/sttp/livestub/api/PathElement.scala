package sttp.livestub.api

sealed trait PathElement {
  def matches(requestPath: List[RequestPath]): (MatchResult, List[RequestPath])
}

object PathElement {
  case class Fixed(path: String) extends PathElement {
    override def matches(requestPath: List[RequestPath]): (MatchResult, List[RequestPath]) = requestPath match {
      case head :: tail if head.path == path => MatchResult.FixedMatch -> tail
      case other                             => MatchResult.NotMatched -> other
    }
  }
  case object Wildcard extends PathElement {
    override def matches(requestPath: List[RequestPath]): (MatchResult, List[RequestPath]) = {
      requestPath match {
        case _ :: tail => MatchResult.WildcardMatch -> tail
        case other     => MatchResult.NotMatched -> other
      }
    }
  }
  case object MultiWildcard extends PathElement {
    override def matches(requestPath: List[RequestPath]): (MatchResult, List[RequestPath]) =
      MatchResult.MultiWildcardMatch -> Nil
  }

  def fromString(strPath: String): PathElement = {
    strPath match {
      case "*"   => PathElement.Wildcard
      case "**"  => PathElement.MultiWildcard
      case other => PathElement.Fixed(other)
    }
  }
}
