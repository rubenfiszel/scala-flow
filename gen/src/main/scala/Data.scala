package spatial.fusion.gen


trait Data[A] {
  def toValues(x: A): Seq[Real]
}

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

case class Cache[A](source: Source[A]) extends Source[A] {
  lazy val cStream = source.stream()
  def stream()     = cStream
}

//case class Functor[A, F[_]](source: Source[F[A]], fmap: A => B) extends Source[F[B]] {
//  def stream() = source.stream()
//}
//case class Buffer2(source1: Source[Time], source2: Source[Timestamped[A]], source3: Source[Timestamped[B]]) with Source[(Stream[Times

case class Reduce[A](source: Source[Stream[A]], r: (A, A) => A)
    extends FlatMap[Stream[A], A] {
  def f(x: Stream[A]): Stream[A] =
    if (!x.isEmpty)
      Stream(x.reduce(r))
    else
      Stream()
}

case class PrintSink[A](source: Source[A]) extends Sink[A] {
  def f(x: A) = println(x)
}

