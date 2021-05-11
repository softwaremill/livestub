package sttp.livestub.api

case class RequestStub(method: MethodStub, url: RequestPathAndQuery) {
  def matches(request: Request): MatchResult = {
    method.matches(request.method).combine(url.matches(request.paths, request.queries))
  }
}
object RequestStub {
  def apply(method: MethodStub, url: String): RequestStub = {
    new RequestStub(method, RequestPathAndQuery.fromString(url))
  }
}
