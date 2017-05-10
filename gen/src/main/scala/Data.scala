package spatial.fusion.gen

import breeze.stats.distributions._

trait Data[A] {
  def toValues(x: A): Seq[Real]
}

case class Timestamped[+A](t: Time, v: A)

trait Source[A] {
  def stream(): Stream[A]
}

trait Op[A] {
  def source: Source[A]
}

trait Op2[A, B] {
  def source1: Source[A]
  def source2: Source[B]
}

trait Op3[A, B, C] {
  def source1: Source[A]
  def source2: Source[B]
  def source3: Source[C]
}

trait Map[A, B] extends Op[A] with Source[B] {
  def f(x: A): B
  def stream(): Stream[B] = source.stream().map(f)
}

trait FlatMap[A, B] extends Op[A] with Source[B] {
  def f(x: A): Stream[B]
  def stream(): Stream[B] = source.stream().flatMap(f)
}

trait TakeWhile[A] extends Op[A] with Source[A] {
  def f(x: A): Boolean
  def stream() = source.stream().takeWhile(f)
}

trait Sink[A] extends Op[A] {
  def f(x: A): Unit
  def consumeAll() =
    source.stream().foreach(f)
}

case class Buffer[A](source1: Source[Time], source2: Source[Timestamped[A]])
    extends Op2[Time, Timestamped[A]]
    with Source[Stream[Timestamped[A]]] {

  var i  = 0
  var s1 = source1.stream()
  var s2 = source2.stream()
  def stream(): Stream[Stream[Timestamped[A]]] =
    streamT(s1.head) #:: streamTail()
  def streamTail(): Stream[Stream[Timestamped[A]]] = {
    s1 = s1.tail
    if (s2.isEmpty)
      Stream()
    else if (s1.isEmpty)
      Stream(s2)
    else
      stream()
  }

  def streamT(stop: Time): Stream[Timestamped[A]] = {
    val (pre, su) = s2.span(_.t < stop)
    s2 = su
    pre
  }

}

case class Zip2[A, B](source1: Source[A], source2: Source[B])
    extends Op2[A, B]
    with Source[(A, B)] {
  def stream() = source1.stream().zip(source2.stream())
}

case class Zip3[A, B, C](source1: Source[A],
                         source2: Source[B],
                         source3: Source[C])
    extends Op3[A, B, C]
    with Source[(A, B, C)] {
  def stream() =
    source1.stream().zip(source2.stream()).zip(source3.stream()).map {
      case ((a, b), c) => (a, b, c)
    }
}

object Combine2 {

  def apply[A, B](source1: Source[Time],
                  source2: Source[Timestamped[A]],
                  source3: Source[Timestamped[B]])
      : Source[(Stream[Timestamped[A]], Stream[Timestamped[B]])] = {
    Zip2(Buffer(source1, source2), Buffer(source1, source3))
  }
}


case class Cache[A](source: Source[A]) extends Source[A] {
  lazy val cStream = source.stream()
  def stream()     = cStream
}

//case class Buffer2(source1: Source[Time], source2: Source[Timestamped[A]], source3: Source[Timestamped[B]]) with Source[(Stream[Times

case class Reduce[A](source: Source[Stream[A]], r: (A, A) => A)
    extends FlatMap[Stream[A], A] {
  def f(x: Stream[A]): Stream[A] =
    if (!x.isEmpty)
      Stream(x.reduce(r))
    else
      Stream()
}

case class TimestampFunctor[A, B](source: Source[Timestamped[A]], fmap: A => B)
    extends Map[Timestamped[A], Timestamped[B]] {
  def f(x: Timestamped[A]) = x.copy(v = fmap(x.v))
}
case class PrintSink[A](source: Source[A]) extends Sink[A] {
  def f(x: A) = println(x)
}

case class Clock(dt: Timestep) extends Source[Time] {
  def stream() = genPerfectTimes(dt)
}

case class ClockStop(source: Source[Time], tf: Time) extends TakeWhile[Time] {
  def f(x: Time) = x < tf
}

case class ClockVar(source: Source[Time], std: Timestep)
    extends Map[Time, Time] {
  def f(x: Time) = Gaussian(x, std)(Random).draw()
}
