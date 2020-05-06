package sttp.livestub.api

import io.circe.{Encoder, Json}
import sttp.model.{Header, StatusCode}

case class Response(body: Option[Json] = None, statusCode: StatusCode, headers: List[Header] = List.empty)
object Response {
  def withBody[T: Encoder](body: T, statusCode: StatusCode, headers: List[Header] = List.empty): Response = {
    new Response(Some(implicitly[Encoder[T]].apply(body)), statusCode, headers)
  }
  def emptyBody(statusCode: StatusCode, headers: List[Header] = List.empty): Response = {
    new Response(None, statusCode, headers)
  }

  def withJsonBody(body: Json, statusCode: StatusCode, headers: List[Header] = List.empty): Response = {
    new Response(Some(body), statusCode, headers)
  }
}
