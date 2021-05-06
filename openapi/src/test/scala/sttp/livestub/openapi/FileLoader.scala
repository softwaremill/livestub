package sttp.livestub.openapi

object FileLoader {
  def loadFile(fileName: String): String =
    try scala.io.Source
      .fromResource(fileName)
      .getLines()
      .mkString("\n")
    catch {
      case ex: Throwable =>
        println(s"Error while loading file $fileName")
        throw ex
    }
}
