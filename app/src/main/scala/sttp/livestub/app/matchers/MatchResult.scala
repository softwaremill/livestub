package sttp.livestub.app.matchers

sealed trait MatchResult {
  def combine(other: MatchResult): MatchResult
}
object MatchResult {
  case object NotMatched extends MatchResult {
    override def combine(other: MatchResult): MatchResult = NotMatched
  }
  case object FixedMatch extends MatchResult {
    override def combine(other: MatchResult): MatchResult = {
      other match {
        case NotMatched         => NotMatched
        case FixedMatch         => FixedMatch
        case WildcardMatch      => WildcardMatch
        case MultiWildcardMatch => MultiWildcardMatch
      }
    }
  }
  case object WildcardMatch extends MatchResult {
    override def combine(other: MatchResult): MatchResult = {
      other match {
        case NotMatched         => NotMatched
        case FixedMatch         => WildcardMatch
        case WildcardMatch      => WildcardMatch
        case MultiWildcardMatch => WildcardMatch
      }
    }
  }
  case object MultiWildcardMatch extends MatchResult {
    override def combine(other: MatchResult): MatchResult = {
      other match {
        case NotMatched         => NotMatched
        case FixedMatch         => MultiWildcardMatch
        case WildcardMatch      => MultiWildcardMatch
        case MultiWildcardMatch => MultiWildcardMatch
      }
    }
  }
}
