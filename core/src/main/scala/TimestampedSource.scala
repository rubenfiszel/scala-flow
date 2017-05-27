package dawn.flow

import io.circe.generic.JsonCodec

trait Timestamped[A] {
  self =>
  def v: A
  def time: Time
  def map[B](f: A => B) = Timestamped[B](time, f(v))
  def addLatency(dt: Time) = Timestamped[A](time + dt, v)
  override def toString = s"TS($time, $v)"
}

object Timestamped {

  def apply[A](t1: Time, v1: => A): Timestamped[A] = new Timestamped[A] {
    lazy val v = v1
    def time = t1
  }

  def apply[A](v1: => A): Timestamped[A] = apply(0, v1)

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

