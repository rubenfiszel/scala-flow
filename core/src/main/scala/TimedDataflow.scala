 package dawn.flow

import breeze.stats.distributions._
import io.circe.generic.JsonCodec

@JsonCodec
case class Timestamped[A](t: Time, v: A, dt: Timestep = 0) {
  def time = t + dt
}

case class Buffer[A, B](source1: Source[Time, B], source2: SourceT[A, B])
    extends Op2[Time, Timestamped[A], B]
    with Source[StreamT[A], B] {

  def stream(p: B): Stream[StreamT[A]] = {
    var i  = 0
    var s1 = source1.stream(p)
    var s2 = source2.stream(p)
    
    def streamTail(): Stream[StreamT[A]] = {
      s1 = s1.tail
      if (s2.isEmpty)
        Stream()
      else if (s1.isEmpty)
        Stream(s2)
      else
        rec()
    }

    def streamT(stop: Time): StreamT[A] = {
      val (pre, su) = s2.span(_.t < stop)
      s2 = su
      pre
    }

    def rec(): Stream[StreamT[A]] =
      streamT(s1.head) #:: streamTail()

    rec()
  }

}

case class TimestampFunctor[A, B, C](source: SourceT[A, C], fmap: A => B)
    extends MapT[A, B, C] {
  def f(p: C, x: Timestamped[A]) = x.copy(v = fmap(x.v))
}


case class Clock(dt: Timestep) extends Source[Time, Null] {
  def stream(p: Null) = genPerfectTimes(dt)
}

case class ClockStop(source: Source[Time, Null], tf: Timeframe) extends TakeWhile[Time, Null] {
  def f(p: Null, x: Time) = x < tf
}

case class Latency[A, B](source: SourceT[A, B], dt: Timestep) extends MapT[A, A, B] {
  def f(p: B, x: Timestamped[A]) = x.copy(dt = this.dt + dt) 
}

case class ClockVar(source: Source[Time, Nothing], std: Timestep)
    extends Map[Time, Time, Nothing] {
  def f(p: Nothing, x: Time) = Gaussian(x, std)(Random).draw()
}

object Clock {
  def apply[A](dt: Timestep, tf: Timeframe): Source[Time, A] =
    ClockStop(Clock(dt), tf)
}

object Combine2 {
  def apply[A, B, C](source1: Source[Time, C],
                  source2: SourceT[A, C],
                  source3: SourceT[B, C])
      : Source[(StreamT[A], StreamT[B]), C] = {
    Zip2(Buffer(source1, source2), Buffer(source1, source3))
  }
}

trait SinkTimstamped[A, B] extends Sink[B] {

  def isEmpty: Boolean
  def consume(p: B): Unit =
    ???

  def next: Unit
  def current: Option[Timestamped[A]]
  def f(x: Timestamped[A]): Unit


}
