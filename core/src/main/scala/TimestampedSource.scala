package dawn.flow

import io.circe.generic.JsonCodec

trait Timestamped[A] {
  self =>
  def v: A
  def t: Time
  def dt: Time
  def time = t + dt
  def map[B](f: A => B) = Timestamped[B](t, f(v), dt)
  def addLatency(dt1: Time) = Timestamped[A](t, v, dt + dt1)
}

object Timestamped {

  def apply[A](t1: Time, v1: => A,dt1: Time = 0): Timestamped[A] = new Timestamped[A] {
    lazy val v = v1
    def t = t1
    def dt = dt1
  }

  def apply[A](v1: => A): Timestamped[A] = apply(0, v1)

}


class TimestampedSource[A](source: Source[Timestamped[A]]) {

  def mapT[B](f: (A) => B, name: String = ""): SourceT[B] =
    source.map(
      (x: Timestamped[A]) => x.map(f),
      "FunctorT " + getStrOrElse(name, f.toString))


  def zipT[C](s2: SourceT[C]) =
    source.zip(s2).map(x => Timestamped(x._1.t, (x._1.v, x._2.v), x._1.dt))

  def zipLast[C](source2: SourceT[C]) = {
    def stream(s1: StreamT[A], s2: StreamT[C]): Stream[(Timestamped[A], Timestamped[C])]= {
      if (s1.isEmpty || s2.isEmpty)
        Stream.empty
      else {
//        if (s1.head.time > s2.head.time)
//          stream(s1, s2.tail)
//        else 
          (s1.head, s2.head) #:: stream(s1.tail, s2.tail)
      }      
    }
    source.from2Stream(source2, (s1: StreamT[A], s2: StreamT[C]) => stream(s1, s2), "ZipLast")    
  }

  def zipLastT[C](s2: SourceT[C]) =
    zipLast(s2).map(x => Timestamped(x._1.t, (x._1.v, x._2.v), x._1.dt), "ZipLastT")

  def latency(dt1: Timestep) =
    source.map(
      (x: Timestamped[A]) => x.addLatency(dt1),
                     "Latency " + dt1)

  def accumulate(time: Source[Time]) =
    time.accumulate(source)

  def combine[C](time: Source[Time], source2: SourceT[C]) =
    time.combine(source, source2)

  def toTime =
    source.map(_.time)

  def merge[C](source2: SourceT[C]): Source[Either[Timestamped[A], Timestamped[C]]] = {
    def mergeR(s1: StreamT[A], s2: StreamT[C]): Stream[Either[Timestamped[A], Timestamped[C]]] = { 
      if (!s1.isEmpty && !s2.isEmpty) {
        if (s1.head.time <= s2.head.time)
          Left(s1.head) #:: mergeR(s1.tail, s2)
        else
          Right(s2.head) #:: mergeR(s1, s2.tail)
      }
      else if (s2.isEmpty)
        s1.map(Left(_))
      else
        s2.map(Right(_))
    }
      
    source.from2Stream(source2, (s1: StreamT[A], s2: StreamT[C]) => mergeR(s1, s2), "Zip")
  }

  def fusion(sources: SourceT[A]*): SourceT[A] = {
    def flatten(x: Either[Timestamped[A], Timestamped[A]]): Timestamped[A] =
      x match {
        case Left(y) => y
        case Right(y) => y
      }
    sources.foldLeft(source)((acc, pos) => acc.merge(pos).map(flatten _, "Fusion"))
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
