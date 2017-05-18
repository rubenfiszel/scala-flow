package dawn.flow

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

  def flatMap[C](f: (A) => Stream[C]) =
    fromStream(_.flatMap(f), "FlatMap " + f.toString)
  def flatMap[C](f: (B, A) => Stream[C]) =
    fromStream((p: B, s: Stream[A]) => s.flatMap(x => f(p, x)),
               "FlatMap2 " + f.toString)

  def zip[C](s2: Source[C, B]) =
    from2Stream(s2, (p: B, s1: Stream[A]) => s1.zip(s2.stream(p)), "Zip")

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

case class NamedFunction1[A, B](f: A => B, name: String) extends (A => B) {
  override def toString = name
  def apply(x: A)       = f(x)
}

case class NamedFunction2[A, B, C](f: (A, B) => C, name: String) extends ((A, B) => C) {
  override def toString = name
  def apply(x: A, y: B) = f(x, y)
}


trait Resettable {
  def reset(): Unit
}
