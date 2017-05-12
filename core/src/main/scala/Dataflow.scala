package dawn.flow

object Source {
  def apply[A, B](stream1: B => Stream[A]) = new Source[A, B] {
    def stream(param1: B) = stream1(param1)
  }
}

trait Source[A, B] {

  def stream(param: B): Stream[A]

  def fromStream[C](f: (B, Stream[A]) => Stream[C]) =
    Source.apply((p: B) => f(p, stream(p)))
  def fromStream[C](f: (Stream[A]) => Stream[C]) =
    Source.apply((p: B) => f(stream(p)))

  def filter(b: (A) => Boolean) = fromStream(_.filter(b))
  def filter(b: (B, A) => Boolean) =
    fromStream((p: B, s: Stream[A]) => s.filter(x => b(p, x)))

  def takeWhile(b: (A) => Boolean) = fromStream(_.takeWhile(b))
  def takeWhile(b: (B, A) => Boolean) =
    fromStream((p: B, s: Stream[A]) => s.takeWhile(x => b(p, x)))

  def map[C](f: (A) => C) = fromStream(_.map(f))
  def map[C](f: (B, A) => C) =
    fromStream((p: B, s: Stream[A]) => s.map(x => f(p, x)))

  def mapT[C, D](f: (B, C) => D)(implicit asC: A <:< Timestamped[C]) =
    map(asC).map((p: B, x: Timestamped[C]) => x.copy(v = f(p, x.v)))

  def mapT[C, D](f: (C) => D)(implicit asC: A <:< Timestamped[C]) =
    map(asC).map((x: Timestamped[C]) => x.copy(v = f(x.v)))

  def flatMap[C](f: (A) => Stream[C]) = fromStream(_.flatMap(f))
  def flatMap[C](f: (B, A) => Stream[C]) =
    fromStream((p: B, s: Stream[A]) => s.flatMap(x => f(p, x)))

}

trait Sink[B] {
  def consumeAll(p: B): Unit
}

trait SinkP[B] extends Sink[B] {
  def isEmpty: Boolean
  def consume(p: B): Unit
  def consumeAll(p: B) =
    while (!isEmpty) consume(p)
}

trait Sink1[A, B] extends SinkP[B] {

  def source: Source[A, B]
  def f(p: B, x: A): Unit

  var iterator: Option[Iterator[A]] = None

  def isEmpty = iterator.isDefined && !iterator.get.hasNext

  def consume(p: B) = {
    if (!iterator.isDefined)
      iterator = Some(source.stream(p).toIterator)

    if (iterator.get.hasNext) {
      val n = iterator.get.next
      f(p, n)
    }

  }
}

trait Sink2[A, B, C] extends SinkP[C] {

  def source1: Source[A, C]
  def source2: Source[B, C]

  def f1(p: C, x: A): Unit
  def f2(p: C, x: B): Unit

  val s1 = new Sink1[A, C] {
    def source        = source1
    def f(p: C, x: A) = f1(p, x)
  }

  val s2 = new Sink1[B, C] {
    def source        = source2
    def f(p: C, x: B) = f2(p, x)
  }

  def isEmpty = s1.isEmpty && s2.isEmpty

  def consume(p: C) = {
    s1.consume(p)
    s2.consume(p)
  }
}

case class Zip2[A, B, C](source1: Source[A, C], source2: Source[B, C])
    extends Source[(A, B), C] {
  def stream(p: C) = source1.stream(p).zip(source2.stream(p))
}

case class Zip3[A, B, C, D](source1: Source[A, D],
                            source2: Source[B, D],
                            source3: Source[C, D])
    extends Source[(A, B, C), D] {
  def stream(p: D) =
    source1.stream(p).zip(source2.stream(p)).zip(source3.stream(p)).map {
      case ((a, b), c) => (a, b, c)
    }
}

case class Cache[A, B](source: Source[A, B]) extends Source[A, B] {
  var cStream: Option[Stream[A]] = None
  def stream(p: B) = {
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
/*
case class Reduce[A, B](source: Source[Stream[A], B], r: (A, A) => A)
    extends Source[A,B] {
  def f(p: B, x: Stream[A]): Stream[A] =
    if (!x.isEmpty)
      Stream(x.reduce(r))
    else
      Stream()

  def stream() =
}
 */

case class PrintSink[A, B](source: Source[A, B]) extends Sink1[A, B] {
  def f(p: B, x: A) = println(x)
}
