package sttp.livestub.app

import cats.effect.Sync
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

private[app] trait FLogger { outer =>
  implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLoggerFromClass[F](outer.getClass)
}
