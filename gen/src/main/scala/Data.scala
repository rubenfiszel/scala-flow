package spatial.fusion.gen


trait Data[A] {
  def toValues(x: A): Seq[Real]
}

trait Source[A, B] {
  def stream(param: B): Stream[A]
}

trait Op[A, B] {
  def source: Source[A, B]
}

trait Op2[A, B, C] {
  def source1: Source[A, C]
  def source2: Source[B, C]
}

trait Op3[A, B, C, D] {
  def source1: Source[A, D]
  def source2: Source[B, D]
  def source3: Source[C, D]
}

trait Map[A, B, C] extends Op[A, C] with Source[B, C] {
  def f(p:C, x: A): B
  def stream(p: C): Stream[B] = source.stream(p).map(x => f(p, x))
}

trait FlatMap[A, B, C] extends Op[A, C] with Source[B, C] {
  def f(p:C, x: A): Stream[B]
  def stream(p: C): Stream[B] = source.stream(p).flatMap(x => f(p, x))
}

trait TakeWhile[A, B] extends Op[A, B] with Source[A, B] {
  def f(p: B, x: A): Boolean
  def stream(p: B) = source.stream(p).takeWhile(x => f(p, x))
}

trait Sink[A, B] extends Op[A, B] {
  def f(p:B, x: A): Unit
  def consumeAll(p: B) =
    source.stream(p).foreach(x => f(p, x))
}


case class Zip2[A, B, C](source1: Source[A, C], source2: Source[B, C])
    extends Op2[A, B, C]
    with Source[(A, B), C] {
  def stream(p: C) = source1.stream(p).zip(source2.stream(p))
}

case class Zip3[A, B, C, D](source1: Source[A, D],
                         source2: Source[B, D],
                         source3: Source[C, D])
    extends Op3[A, B, C, D]
    with Source[(A, B, C), D] {
  def stream(p: D) =
    source1.stream(p).zip(source2.stream(p)).zip(source3.stream(p)).map {
      case ((a, b), c) => (a, b, c)
    }
}

case class Cache[A, B](source: Source[A, B]) extends Source[A, B] {
  var cStream: Option[Stream[A]] = None
  def stream(p: B)  = {
    if (cStream.isEmpty) {
      cStream = Some(source.stream(p))
    }
    cStream.get
  }
}

//case class Functor[A, F[_]](source: Source[F[A]], fmap: A => B) extends Source[F[B]] {
//  def stream() = source.stream()
//}
//case class Buffer2(source1: Source[Time], source2: Source[Timestamped[A]], source3: Source[Timestamped[B]]) with Source[(Stream[Times

case class Reduce[A, B](source: Source[Stream[A], B], r: (A, A) => A)
    extends FlatMap[Stream[A], A, B] {
  def f(p: B, x: Stream[A]): Stream[A] =
    if (!x.isEmpty)
      Stream(x.reduce(r))
    else
      Stream()
}

case class PrintSink[A, B](source: Source[A, B]) extends Sink[A, B] {
  def f(p: B, x: A) = println(x)
}

