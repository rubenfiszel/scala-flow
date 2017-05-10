package spatial.fusion.gen

import breeze.stats.distributions._

case class Timestamped[+A](t: Time, v: A, dt: Timestep = 0) {
  def time = t + dt
}

case class Buffer[A](source1: Source[Time], source2: SourceT[A])
    extends Op2[Time, Timestamped[A]]
    with Source[StreamT[A]] {

  var i  = 0
  var s1 = source1.stream()
  var s2 = source2.stream()
  def stream(): Stream[StreamT[A]] =
    streamT(s1.head) #:: streamTail()
  def streamTail(): Stream[StreamT[A]] = {
    s1 = s1.tail
    if (s2.isEmpty)
      Stream()
    else if (s1.isEmpty)
      Stream(s2)
    else
      stream()
  }

  def streamT(stop: Time): StreamT[A] = {
    val (pre, su) = s2.span(_.t < stop)
    s2 = su
    pre
  }

}

case class TimestampFunctor[A, B](source: SourceT[A], fmap: A => B)
    extends MapT[A, B] {
  def f(x: Timestamped[A]) = x.copy(v = fmap(x.v))
}


case class Clock(dt: Timestep) extends Source[Time] {
  def stream() = genPerfectTimes(dt)
}

case class ClockStop(source: Source[Time], tf: Timeframe) extends TakeWhile[Time] {
  def f(x: Time) = x < tf
}

case class Latency[A](source: SourceT[A], dt: Timestep) extends MapT[A, A] {
  def f(x: Timestamped[A]) = x.copy(dt = this.dt + dt) 
}

case class ClockVar(source: Source[Time], std: Timestep)
    extends Map[Time, Time] {
  def f(x: Time) = Gaussian(x, std)(Random).draw()
}

object Clock {
  def apply(dt: Timestep, tf: Timeframe): Source[Time] =
    ClockStop(Clock(dt), tf)
}

object Combine2 {
  def apply[A, B](source1: Source[Time],
                  source2: SourceT[A],
                  source3: SourceT[B])
      : Source[(StreamT[A], StreamT[B])] = {
    Zip2(Buffer(source1, source2), Buffer(source1, source3))
  }
}

trait SinkT[A] extends Sink[Timestamped[A]] {

  def fScheduled(x: Timestamped[A]): Time = {    
    f(x)
    x.time
  }
}
