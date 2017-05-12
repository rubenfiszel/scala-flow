package dawn.flow

trait Source[A, B] extends Sourcable {

  def name: String = "Default"
  
  def stream(param: B): Stream[A]

  def fromStream[C](f: (B, Stream[A]) => Stream[C]) =
    Op1.apply(this, (p: B) => f(p, stream(p)))
  def fromStream[C](f: (Stream[A]) => Stream[C]) =
    Op1.apply(this, (p: B) => f(stream(p)))

  def cache() = Cache(this)

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
    map(asC).map(x => x.copy(v = f(x.v)))

  
  def flatMap[C](f: (A) => Stream[C]) = fromStream(_.flatMap(f))
  def flatMap[C](f: (B, A) => Stream[C]) =
    fromStream((p: B, s: Stream[A]) => s.flatMap(x => f(p, x)))

  def reduceF[C](r: (C, C) => C)(implicit asC: A <:< Stream[C]) =
    map(asC).map(_.reduce(r))
  def reduceF[C](r: (B, C, C) => C)(implicit asC: A <:< Stream[C]) =
    map(asC).map((p: B, s: Stream[C]) => s.reduce(Function.uncurried(r.curried(p))))

  def zip[C](s2: Source[C, B]) =
    fromStream((p: B, s1: Stream[A]) => s1.zip(s2.stream(p)))

  def latency[C](dt1: Timestep)(implicit asC: A <:< Timestamped[C]) =
    map(asC).map(x => x.copy(dt = x.dt + dt1))

  def buffer[C](time: Source[Time, B])(implicit asC: A <:< Timestamped[C]) =
    Buffer(time, this.map(asC))

  def combine[C, D](time: Source[Time, B], source2: SourceT[D, B])(implicit asC: A <:< Timestamped[C]) =
    Combine(time, this.map(asC), source2)
  
}

trait Sourcable {
  def sources: Seq[Source[_, _]]
  def printRec: Unit = {
    sources.foreach(println)
    sources.foreach(_.printRec)
  }
}

trait Source1[A, B] extends Sourcable {
  def source: Source[A, B]
  lazy val sources = List(source)
}

trait Source2[A, B, C] extends Sourcable {
  def source1: Source[A, C]
  def source2: Source[B, C]
  lazy val sources = List(source1, source2)
}

trait Op1[A, B, C] extends Source[A, B] with Source1[C, B]

object Op1 {
  def apply[A, B, C](source1: Source[C, B], stream1: B => Stream[A]) = new Op1[A, B, C] {
    def source = source1
    def stream(param1: B) = stream1(param1)
  }
}


trait Op2[A, B, C, D] extends Source[A, B] with Source2[C, D, B]

trait Sink[B] extends Sourcable {
  def consumeAll(p: B): Unit
}

trait SinkP[B] extends Sink[B] {
  def isEmpty: Boolean
  def consume(p: B): Unit
  def consumeAll(p: B) =
    while (!isEmpty) consume(p)
}

trait Sink1[A, B] extends SinkP[B] with Source1[A, B]{

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

trait Sink2[A, B, C] extends SinkP[C] with Source2[A, B, C]{

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


case class Cache[A, B](source: Source[A, B]) extends Op1[A, B, A] {
  var cStream: Option[Stream[A]] = None
  def stream(p: B) = {
    if (cStream.isEmpty) {
      cStream = Some(source.stream(p))
    }
    cStream.get
  }
}

