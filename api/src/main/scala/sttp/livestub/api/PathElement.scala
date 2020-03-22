package sttp.livestub.api

sealed trait PathElement

object PathElement {
  case class Fixed(path: String) extends PathElement
  case object Wildcard extends PathElement
  case object MultiWildcard extends PathElement

  def fromString(strPath: String): PathElement = {
    strPath match {
      case "*"   => PathElement.Wildcard
      case "**"  => PathElement.MultiWildcard
      case other => PathElement.Fixed(other)
    }
  }
}
