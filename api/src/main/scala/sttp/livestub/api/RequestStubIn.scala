package sttp.livestub.api

case class RequestStubIn(method: MethodStub, url: RequestPathAndQuery)
object RequestStubIn {
  def apply(method: MethodStub, url: String): RequestStubIn = {
    new RequestStubIn(method, RequestPathAndQuery.fromString(url))
  }
}
