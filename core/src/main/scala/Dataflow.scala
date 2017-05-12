package dawn.flow

trait Source[A, B] {

  def stream(param: B): Stream[A]

  def filter(b: (B, A) => Boolean) = Filter(this, b)
  def filter(b: (A) => Boolean) = Filter(this, b)  

  def takeWhile(b: (B, A) => Boolean) = TakeWhile(this, b)
  def takeWhile(b: (A) => Boolean) = TakeWhile(this, b)

  def map[C](f: (B, A) => C) = Map(this, f)
  def map[C](f: (A) => C) = Map(this, f)

  def mapT[C, D](f: (B, C) => D)(implicit asC: A <:< Timestamped[C]) = MapT[C, D, B](map(asC), f)
  def mapT[C, D](f: (C) => D)(implicit asC: A <:< Timestamped[C]) = MapT[C, D, B](map(asC), f)  

  def flatMap[C](f: (B, A) => Stream[C]) = FlatMap(this, f)
  def flatMap[C](f: (A) => Stream[C]) = FlatMap(this, f)  

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
  def f(p: C, x: A): B
  def stream(p: C): Stream[B] = {
    source.stream(p).map(x => f(p, x))
  }
}

object Map {
  def apply[A, B, C](source1: Source[A, C], fun: (C, A) => B) = new Map[A, B, C] {
    def source = source1
    def f(p: C, x: A) = fun(p, x)
  }

  def apply[A, B, C](source1: Source[A, C], fun: (A) => B): Map[A, B, C] = apply(source1, (p: C, x: A) => fun(x))    
}

trait FlatMap[A, B, C] extends Op[A, C] with Source[B, C] {
  def f(p: C, x: A): Stream[B]
  def stream(p: C): Stream[B] = source.stream(p).flatMap(x => f(p, x))
}

object FlatMap {

  def apply[A, B, C](source1: Source[A, C], fun: (C, A) => Stream[B]) = new FlatMap[A, B, C] {
    def source = source1
    def f(p: C, x: A) = fun(p, x)
  }

  def apply[A, B, C](source1: Source[A, C], fun: (A) => Stream[B]): FlatMap[A, B, C] = apply(source1, (p: C, x: A) => fun(x))  
}

trait Filter[A, B] extends Op[A, B] with Source[A, B] {
  def b(p: B, x: A): Boolean
  def stream(p: B): Stream[A] = source.stream(p).filter(x => b(p, x))
}

object Filter {

  def apply[A, B](source1: Source[A, B], fun: (B, A) => Boolean) = new Filter[A, B] {
    def source = source1
    def b(p: B, x: A) = fun(p, x)
  }

  def apply[A, B](source1: Source[A, B], fun: (A) => Boolean): Filter[A, B] = apply(source1, (p: B, x: A) => fun(x))

}

trait TakeWhile[A, B] extends Op[A, B] with Source[A, B] {
  def f(p: B, x: A): Boolean
  def stream(p: B) = source.stream(p).takeWhile(x => f(p, x))
}

object TakeWhile {

  def apply[A, B](source1: Source[A, B], fun: (B, A) => Boolean): TakeWhile[A, B] = new TakeWhile[A, B] {
    def source = source1
    def f(p: B, x: A) = fun(p, x)
  }

  def apply[A, B](source1: Source[A, B], fun: (A) => Boolean): TakeWhile[A, B] = apply(source1, (p: B, x: A) => fun(x))
  
}


trait Sink[B] {
  def consumeAll(p: B): Unit
}

trait SinkP[B] extends Sink[B] {
  def isEmpty: Boolean
  def consume(p: B): Unit
  def consumeAll(p: B) =
    while (!isEmpty)
      consume(p)
}

trait Sink1[A, B] extends Op[A, B] with SinkP[B] {

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

trait Sink2[A, B, C] extends Op2[A, B, C] with SinkP[C]{

  def f1(p: C, x: A): Unit
  def f2(p: C, x: B): Unit

  val s1 = new Sink1[A, C] {
    def source = source1
    def f(p: C, x: A) = f1(p, x)    
  }

  val s2 = new Sink1[B, C] {
    def source = source2
    def f(p: C, x: B) = f2(p, x)
  }

  def isEmpty = s1.isEmpty && s2.isEmpty

  def consume(p: C) = {
    s1.consume(p)
    s2.consume(p)    
  }
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

case class Reduce[A, B](source: Source[Stream[A], B], r: (A, A) => A)
    extends FlatMap[Stream[A], A, B] {
  def f(p: B, x: Stream[A]): Stream[A] =
    if (!x.isEmpty)
      Stream(x.reduce(r))
    else
      Stream()
}

case class PrintSink[A, B](source: Source[A, B]) extends Sink1[A, B] {
  def f(p: B, x: A) = println(x)
}
