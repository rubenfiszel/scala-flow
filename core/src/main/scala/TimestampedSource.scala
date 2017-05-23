package dawn.flow

import io.circe.generic.JsonCodec

@JsonCodec
case class Timestamped[A](t: Time, v: A, dt: Timestep = 0) {
  def time = t + dt
}

object Timestamped {

  def apply[A](x: A): Timestamped[A] = Timestamped(0, x)

}


class TimestampedSource[A](source: Source[Timestamped[A]]) {

  def mapT[B](f: (A) => B, name: String = ""): SourceT[B] =
    source.map(
      (x: Timestamped[A]) => x.copy(v = f(x.v)),
      "FunctorT " + getStrOrElse(name, f.toString))


  def zipT[C](s2: SourceT[C]) =
    source.zip(s2).map(x => Timestamped(x._1.t, (x._1.v, x._2.v), x._1.dt))


  def latency(dt1: Timestep) =
    source.map(
      (x: Timestamped[A]) => x.copy(dt = x.dt + dt1),
                     "Latency " + dt1)

  def accumulate(time: Source[Time]) =
    time.accumulate(source)

  def combine[C](time: Source[Time], source2: SourceT[C]) =
    time.combine(source, source2)

  def toTime =
    source.map(_.t)

  def merge[C](source2: Source[Timestamped[C]]) = {
    def mergeR(s1: Stream[Timestamped[A]], s2: Stream[Timestamped[C]]): Stream[Either[Timestamped[A], Timestamped[C]]] = { 
      if (!s1.isEmpty && !s2.isEmpty) {
        if (s1.head.t <= s2.head.t)
          Left(s1.head) #:: mergeR(s1.tail, s2)
        else
          Right(s2.head) #:: mergeR(s1, s2.tail)
      }
      else if (s2.isEmpty)
        s1.map(Left(_))
      else
        s2.map(Right(_))
    }
      
    source.from2Stream(source2, (s1: Stream[Timestamped[A]], s2: Stream[Timestamped[C]]) => mergeR(s1, s2), "Zip")
  }

}


case class Accumulate[A](source1: Source[Time], source2: SourceT[A])
    extends Op2[Time, Timestamped[A], StreamT[A]] {


  def genStream(): Stream[StreamT[A]] = {
    var i  = 0
    var s1 = source1.stream()
    var s2 = source2.stream()

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
      val (pre, su) = s2.span(_.t <= stop)
      s2 = su
      pre
    }

    def rec(): Stream[StreamT[A]] =
      streamT(s1.head) #:: streamTail()

    rec()
  }

}

object Combine {
  def apply[A, B](
      source1: Source[Time],
      source2: SourceT[A],
      source3: SourceT[B]): Source[(StreamT[A], StreamT[B])] = {
    Accumulate(source1, source2).zip(Accumulate(source1, source3))
  }
}
