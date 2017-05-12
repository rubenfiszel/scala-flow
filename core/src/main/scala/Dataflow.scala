package dawn.flow

import com.github.mdr.ascii.graph._

case class NamedFunction1[A, B](f: A => B, name: String) extends (A => B) {
  override def toString = name
  def apply(x: A)       = f(x)
}

case class NamedFunction2[A, B, C](f: (A, B) => C, name: String)
    extends ((A, B) => C) {
  override def toString = name
  def apply(x: A, y: B) = f(x, y)
}

trait Source[A, B] extends Sourcable { parent =>

  def stream(param: B): Stream[A]

  def fromStream[C](f: (B, Stream[A]) => Stream[C], name: String) =
    Op1.apply(this, (p: B) => f(p, stream(p)), name)

  def fromStream[C](f: (Stream[A]) => Stream[C], name: String) =
    Op1.apply(this, (p: B) => f(stream(p)), name)

  def from2Stream[C, D](s2: Source[D, B],
                        f: (B, Stream[A]) => Stream[C],
                        name: String) =
    Op2.apply(this, s2, (p: B) => f(p, stream(p)), name)

  def cache() = Cache(this)

  def filter(b: (A) => Boolean) =
    fromStream(_.filter(b), "Filter " + b.toString)
  def filter(b: (B, A) => Boolean) =
    fromStream((p: B, s: Stream[A]) => s.filter(x => b(p, x)),
               "Filter2 " + b.toString)

  def takeWhile(b: (A) => Boolean) =
    fromStream(_.takeWhile(b), "TakeWhile " + b.toString)
  def takeWhile(b: (B, A) => Boolean) =
    fromStream((p: B, s: Stream[A]) => s.takeWhile(x => b(p, x)),
               "TakeWhile2 " + b.toString)

  def map[C](f: (A) => C) = fromStream(_.map(f), "Map " + f.toString)
  def map[C](f: (B, A) => C) =
    fromStream((p: B, s: Stream[A]) => s.map(x => f(p, x)),
               "Map2 " + f.toString)

  //Divide the frequency of the stream by n
  def divider(n: Int) =
    fromStream((s: Stream[A]) => s.grouped(n).map(_.last).toStream,
               "Divider " + n)

  def silentMap[C](f: (A) => C) = new Source[C, B] {
    def sources           = parent.sources
    override def toString = parent.toString
    def stream(p: B)      = parent.stream(p).map(f)
  }

  def mapT[C, D](f: (B, C) => D)(implicit asC: A <:< Timestamped[C]) =
    silentMap(asC).map(
      NamedFunction2((p: B, x: Timestamped[C]) => x.copy(v = f(p, x.v)),
                     "MapT2 " + f.toString))

  def mapT[C, D](f: (C) => D)(implicit asC: A <:< Timestamped[C]) =
    silentMap(asC).map(
      NamedFunction1((x: Timestamped[C]) => x.copy(v = f(x.v)),
                     "MapT " + f.toString))

  def flatMap[C](f: (A) => Stream[C]) =
    fromStream(_.flatMap(f), "FlatMap " + f.toString)
  def flatMap[C](f: (B, A) => Stream[C]) =
    fromStream((p: B, s: Stream[A]) => s.flatMap(x => f(p, x)),
               "FlatMap2 " + f.toString)

  def reduceF[C](r: (C, C) => C)(implicit asC: A <:< Stream[C]) =
    silentMap(asC).map(
      NamedFunction1((x: Stream[C]) => x.reduce(r), "Reduce " + r.toString))
  def reduceF[C](r: (B, C, C) => C)(implicit asC: A <:< Stream[C]) =
    silentMap(asC).map(
      NamedFunction2((p: B, s: Stream[C]) =>
                       s.reduce(Function.uncurried(r.curried(p))),
                     "Reduce2 " + r.toString))

  def zip[C](s2: Source[C, B]) =
    from2Stream(s2, (p: B, s1: Stream[A]) => s1.zip(s2.stream(p)), "Zip")

  def latency[C](dt1: Timestep)(implicit asC: A <:< Timestamped[C]) =
    silentMap(asC).map(
      NamedFunction1((x: Timestamped[C]) => x.copy(dt = x.dt + dt1),
                     "Latency " + dt1))

  def buffer[C](time: Source[Time, B])(implicit asC: A <:< Timestamped[C]) =
    Buffer(time, this.silentMap(asC))

  def combine[C, D](time: Source[Time, B], source2: SourceT[D, B])(
      implicit asC: A <:< Timestamped[C]) =
    Combine(time, this.silentMap(asC), source2)

}

trait Sourcable { self =>
  def sources: Seq[Source[_, _]]

  override def toString = getClass.getSimpleName
}

object Sourcable {

  def graph(s: Seq[Sourcable], g: Graph[Sourcable] = Graph(Set(), List())): Graph[Sourcable] = {
    s.foldLeft(g.copy(
        vertices = g.vertices ++ s.toSet ++ s.flatMap(_.sources),
        edges  = g.edges ++ s.toList.flatMap(x => x.sources.map(y => (y, x)))
    ))((acc, pos) => graph(pos.sources, acc))
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
  def apply[A, B, C](source1: Source[C, B],
                     stream1: B => Stream[A],
                     name1: String) = new Op1[A, B, C] {
    override def toString = name1
    def source            = source1
    def stream(param1: B) = stream1(param1)
  }
}

trait Op2[A, B, C, D] extends Source[A, B] with Source2[C, D, B]

object Op2 {
  def apply[A, B, C, D](source11: Source[C, B],
                        source12: Source[D, B],
                        stream1: B => Stream[A],
                        name1: String) = new Op2[A, B, C, D] {
    override def toString = name1
    def source1           = source11
    def source2           = source12
    def stream(param1: B) = stream1(param1)
  }
}

trait Sink[B] extends Sourcable {
  def consumeAll(p: B): Unit
}

trait SinkP[B] extends Sink[B] {
  def isEmpty: Boolean
  def consume(p: B): Unit
  def consumeAll(p: B) =
    while (!isEmpty) consume(p)
}

trait Sink1[A, B] extends SinkP[B] with Source1[A, B] {

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

trait Sink2[A, B, C] extends SinkP[C] with Source2[A, B, C] {

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
