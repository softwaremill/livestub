package sttp.livestub.api

import sttp.model.StatusCode
import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir.{Schema, Tapir, Validator}

trait LiveStubTapirSupport extends Tapir {
  implicit val sStatusCode: Schema[StatusCode] = Schema(SInteger)
  implicit val vStatusCode: Validator[StatusCode] = Validator.pass[StatusCode]

  implicit val methodValueSchema: Schema[MethodValue] = Schema(SString)
  implicit val methodValueValidator: Validator[MethodValue] = Validator.pass

  implicit val requestPathAndQuerySchema: Schema[RequestPathAndQuery] = Schema(SString)
  implicit val requestPathAndQueryValidator: Validator[RequestPathAndQuery] = Validator.pass

}
