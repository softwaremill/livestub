package sttp.livestub

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO

class IoMap[K, V]() {
  private val map = new ConcurrentHashMap[K, V]()

  def put(k: K, v: V): IO[Unit] = {
    IO.delay(map.put(k, v))
  }

  def get(k: K): IO[Option[V]] = {
    IO.delay(Option(map.get(k)))
  }
}
