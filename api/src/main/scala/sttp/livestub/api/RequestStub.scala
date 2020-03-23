package sttp.livestub.api

case class RequestStub(method: MethodValue, url: RequestPathAndQuery)
object RequestStub {
  def apply(method: MethodValue, url: String): RequestStub = {
    new RequestStub(method, RequestPathAndQuery.fromString(url))
  }
}
