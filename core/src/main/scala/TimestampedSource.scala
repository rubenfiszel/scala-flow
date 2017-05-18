package dawn.flow

import io.circe.generic.JsonCodec

@JsonCodec
case class Timestamped[A](t: Time, v: A, dt: Timestep = 0) {
  def time = t + dt
}


class TimestampedSource[A, B](source: Source[Timestamped[A], B]) {

  def mapT[C](f: (B, A) => C) =
    source.map(
      NamedFunction2((p: B, x: Timestamped[A]) => x.copy(v = f(p, x.v)),
                     "Functor2 " + f.toString))

  def mapT[C](f: (A) => C) =
    source.map(
      NamedFunction1((x: Timestamped[A]) => x.copy(v = f(x.v)),
                     "FunctorT " + f.toString))

  def latency(dt1: Timestep) =
    source.map(
      NamedFunction1((x: Timestamped[A]) => x.copy(dt = x.dt + dt1),
                     "Latency " + dt1))

  def accumulate(time: Source[Time, B]) =
    Accumulate(time, source)

  def combine[C](time: Source[Time, B], source2: SourceT[C, B]) =
    Combine(time, source, source2)

}


case class Accumulate[A, B](source1: Source[Time, B], source2: SourceT[A, B])
    extends Op2[StreamT[A], B, Time, Timestamped[A]] {


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

object Combine {
  def apply[A, B, C](
      source1: Source[Time, C],
      source2: SourceT[A, C],
      source3: SourceT[B, C]): Source[(StreamT[A], StreamT[B]), C] = {
    Accumulate(source1, source2).zip(Accumulate(source1, source3))
  }
}
