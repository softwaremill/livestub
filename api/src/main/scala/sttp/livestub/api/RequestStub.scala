package sttp.livestub.api

case class RequestStub(method: MethodValue, url: RequestPathAndQuery)
object RequestStub {
  def apply(method: MethodValue, uri: String): RequestStub = {
    new RequestStub(method, RequestPathAndQuery.fromString(uri))
  }
}
