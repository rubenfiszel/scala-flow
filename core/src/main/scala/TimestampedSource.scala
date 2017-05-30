package dawn.flow

import io.circe.generic.JsonCodec

@JsonCodec
case class Timestamped[A](t: Time, v: A, dt: Time = 0) {
  def time = t + dt
  def map[B](f: A => B) = Timestamped(t, f(v), dt)
  def addLatency(dt: Time): Timestamped[A] = copy(dt = this.dt + dt)
}

object Timestamped {
  def apply[A](x: A): Timestamped[A] = Timestamped(0, x)
}

class TimestampedSource[A](source: SourceT[A]) {

  def mapT[B](f: (A) => B, name: String = ""): SourceT[B] =
    source.map(
      (x: Timestamped[A]) => x.map(f),
      "FunctorT " + getStrOrElse(name, f.toString))


  def zipT[C](s2: SourceT[C]) =
    source.zip(s2).map(x => Timestamped(x._1.time, (x._1.v, x._2.v)))

  def zipLastT[C](s2: SourceT[C]) =
    source.zipLast(s2).map(x => Timestamped(x._1.time, (x._1.v, x._2.v)), "ZipLastT")


  def latency(dt: Time) =
    new Op1T[A, A] {
      def rawSource1 = source
      def listen1(x: Timestamped[A]) = {
        broadcast(x.addLatency(dt), dt)
      }
      def name                  = "LatencyT " + dt
    }


  def toTime =
    source.map(_.time)


}

