package dawn.flow

import io.circe.generic.JsonCodec

case class Timestamped[A](time: Time, v: A) {
  def map[B](f: A => B) = Timestamped(time, f(v))
  def addLatency(dt: Time): Timestamped[A] = ???
}

object Timestamped {
  def apply[A](x: A): Timestamped[A] = Timestamped(0, x)
}

class TimestampedSource[A](source: Source[Timestamped[A]]) {

  def mapT[B](f: (A) => B, name: String = ""): SourceT[B] =
    source.map(
      (x: Timestamped[A]) => x.map(f),
      "FunctorT " + getStrOrElse(name, f.toString))


  def zipT[C](s2: SourceT[C]) =
    source.zip(s2).map(x => Timestamped(x._1.time, (x._1.v, x._2.v)))

  def zipLastT[C](s2: SourceT[C]) =
    source.zipLast(s2).map(x => Timestamped(x._1.time, (x._1.v, x._2.v)), "ZipLastT")

  def latency(dt1: Timestep) =
    source.map(
      (x: Timestamped[A]) => x.addLatency(dt1),
                     "Latency " + dt1)


  def toTime =
    source.map(_.time)


}

