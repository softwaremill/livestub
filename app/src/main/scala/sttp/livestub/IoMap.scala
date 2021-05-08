package sttp.livestub

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import cats.implicits._

import scala.jdk.CollectionConverters._

class IoMap[K, V] {
  private val map = new ConcurrentHashMap[K, V]()

  def put(k: K, v: V): IO[Unit] = {
    IO.delay(map.put(k, v))
  }

  def get(k: K): IO[Option[V]] = {
    IO.delay(Option(map.get(k)))
  }

  def clear(): IO[Unit] = {
    IO.delay(map.clear())
  }

  def getOrPut(k: K, v: => V): IO[V] = {
    get(k).flatMap {
      case Some(value) => value.pure[IO]
      case None =>
        val v1 = v // materialize lazy value
        put(k, v1).as(v1)
    }
  }

  def collectFirst[R](f: PartialFunction[(K, V), R]): IO[Option[R]] = {
    IO.delay(map.asScala.collectFirst(f))
  }

  override def toString: String = map.asScala.toString()
}
