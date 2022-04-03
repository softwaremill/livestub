package sttp.livestub.app.repository

import cats.effect.{IO, Ref}

class IoMap[K, V](ref: Ref[IO, Map[K, V]]) {
  def remove(predicate: K => Boolean): IO[Unit] = {
    ref.update(_.filterNot { case (k, _) => predicate(k) })
  }

  def put(k: K, v: V): IO[Unit] = {
    ref.update(m => m + (k -> v))
  }

  def get(k: K): IO[Option[V]] = {
    ref.get.map(_.get(k))
  }

  def clear(): IO[Unit] = {
    ref.set(Map())
  }

  def collect[R](f: PartialFunction[(K, V), R]): IO[List[R]] = {
    ref.get.map(m => m.collect(f).toList)
  }

  def getAll: IO[Map[K, V]] = ref.get
}
