package dawn.flow

trait Source[A, B] extends Sourcable with Resettable { parent =>

  var cStream: Option[Stream[A]] = None
  var lastB: Option[B] = None

  def reset() = {
    cStream = None
    lastB = None
  }

  def genStream(param: B): Stream[A]

  def stream(param: B) = {
    if (!Config.CACHE_BY_DEFAULT || !lastB.exists(_ == param)) {
      cStream = Some(genStream(param))
      lastB = Some(param)
    }
    cStream.get
  }


  def fromStream[C](f: (B, Stream[A]) => Stream[C], name: String) =
    Op1.apply(this, (p: B) => f(p, stream(p)), name)

  def fromStream[C](f: (Stream[A]) => Stream[C], name: String) =
    Op1.apply(this, (p: B) => f(stream(p)), name)

  def from2Stream[C, D](s2: Source[D, B],
                        f: (B, Stream[A], Stream[D]) => Stream[C],
                        name: String) =
    Op2.apply(this, s2, (p: B) => f(p, stream(p), s2.stream(p)), name)


  def filter(b: (A) => Boolean, name: String): Source[A, B] =
    fromStream(_.filter(b), "Filter " + getStrOrElse(name, b.toString))
  def filter(b: (A) => Boolean): Source[A, B] =
    filter(b, "")
  def filter(b: (B, A) => Boolean, name: String): Source[A, B] =
    fromStream((p: B, s: Stream[A]) => s.filter(x => b(p, x)),
      "Filter2 " + getStrOrElse(name, b.toString))
  def filter(b: (B, A) => Boolean): Source[A, B] =
    filter(b, "")

  def takeWhile(b: (A) => Boolean, name: String): Source[A, B] =
    fromStream(_.takeWhile(b), "TakeWhile " + getStrOrElse(name, b.toString))
  def takeWhile(b: (A) => Boolean): Source[A, B] =
    takeWhile(b, "")

  def takeWhile(b: (B, A) => Boolean, name: String): Source[A, B] =
    fromStream((p: B, s: Stream[A]) => s.takeWhile(x => b(p, x)),
      "TakeWhile2 " + getStrOrElse(name, b.toString))
  def takeWhile(b: (B, A) => Boolean): Source[A, B] =
    takeWhile(b, "")


  def map[C](f: (A) => C, name: String): Source[C, B] =
    fromStream(_.map(f), "Map " + getStrOrElse(name, f.toString))
  def map[C](f: (A) => C): Source[C, B] =
    map(f, "")

  def map[C](f: (B, A) => C, name: String): Source[C, B] =
    fromStream((p: B, s: Stream[A]) => s.map(x => f(p, x)),
      "Map2 " + getStrOrElse(name, f.toString))
  def map[C](f: (B, A) => C): Source[C, B] =
    map(f, "")

  //Divide the frequency of the stream by n
  def divider(n: Int) =
    fromStream((s: Stream[A]) => s.grouped(n).map(_.last).toStream,
               "Divider " + n)

  def flatMap[C](f: (A) => Stream[C], name: String): Source[C, B] =
    fromStream(_.flatMap(f), "FlatMap " + getStrOrElse(name, f.toString))
  def flatMap[C](f: (A) => Stream[C]): Source[C, B] =
    flatMap(f, "")
  def flatMap[C](f: (B, A) => Stream[C], name: String): Source[C, B] =
    fromStream((p: B, s: Stream[A]) => s.flatMap(x => f(p, x)),
      "FlatMap2 " + getStrOrElse(name, f.toString))
  def flatMap[C](f: (B, A) => Stream[C]): Source[C, B] =
    flatMap(f, "")

  def zip[C](s2: Source[C, B]) =
    from2Stream(s2, (p: B, s1s: Stream[A], s2s: Stream[C]) => s1s.zip(s2s), "Zip")

  
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

trait Source3[A, B, C, D] extends Sourcable {
  def source1: Source[A, D]
  def source2: Source[B, D]
  def source3: Source[C, D]  
  lazy val sources = List(source1, source2, source3)
}

trait Op1[A, B, C] extends Source[A, B] with Source1[C, B]

object Op1 {
  def apply[A, B, C](source1: Source[C, B],
                     stream1: B => Stream[A],
                     name1: String) = new Op1[A, B, C] {
    override def toString = name1
    def source            = source1
    def genStream(param1: B) = stream1(param1)
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
    def genStream(param1: B) = stream1(param1)
  }
}

trait Op3[A, B, C, D, E] extends Source[A, B] with Source3[C, D, E, B]

trait Resettable {
  def reset(): Unit
}


abstract class NamedFunction1[A, B](f: A => B, name: String) extends (A => B) {
  override def toString = name
  def apply(x: A)       = f(x)
}

abstract class NamedFunction2[A, B, C](f: (A, B) => C, name: String) extends ((A, B) => C) {
  override def toString = name
  def apply(x: A, y: B) = f(x, y)
}
