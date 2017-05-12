package dawn.flow

import breeze.stats.distributions._
import io.circe.generic.JsonCodec

@JsonCodec
case class Timestamped[A](t: Time, v: A, dt: Timestep = 0) {
  def time = t + dt
}


case class Buffer[A, B](source1: Source[Time, B], source2: SourceT[A, B])
    extends Source[StreamT[A], B] {

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

case class Clock(dt: Timestep) extends Source[Time, Null] {
  def stream(p: Null) = genPerfectTimes(dt)
}

object Clock {
  def apply[A](dt: Timestep, tf: Timeframe): Source[Time, A] =
    ClockStop(Clock(dt), tf)

  def apply[A](dt: Timestep, tf: Timeframe, std: Timestep): Source[Time, A] =
    ClockVar(ClockStop(Clock(dt), tf), std)

}

object ClockStop {
  def apply(source: Source[Time, Null], tf: Timeframe) =
    source.takeWhile(_ < tf)
}

object Latency {
  def apply[A, B](source: SourceT[A, B], dt1: Timestep) =
    source.latency(dt1)
}

object ClockVar {
  def apply(source: Source[Time, Null], std: Timestep) =
    source.map(Gaussian(_, std)(Random).draw())
}

object Combine {
  def apply[A, B, C](
      source1: Source[Time, C],
      source2: SourceT[A, C],
      source3: SourceT[B, C]): Source[(StreamT[A], StreamT[B]), C] = {
    Buffer(source1, source2).zip(Buffer(source1, source3))
  }
}

trait SinkTimestamped[A, B] extends Sink[B] {

  def isEmpty: Boolean
  def consume(p: B): Unit =
    ???

  def next: Unit
  def current: Option[Timestamped[A]]
  def f(x: Timestamped[A]): Unit

}
