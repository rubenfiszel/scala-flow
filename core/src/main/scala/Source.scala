package dawn.flow

trait Source[A] extends Sourcable with Resettable { parent =>

  var cStream: Option[Stream[A]] = None

  def reset() = {
    cStream = None
  }

  def genStream(): Stream[A]

  def stream() = {
    if (!Config.CACHE_BY_DEFAULT || !cStream.isDefined) {
      cStream = Some(genStream())
    }
    cStream.get
  }

  def fromStream[C](f: (Stream[A]) => Stream[C], name: String, requireModel: Boolean = false) =
    Op1.apply(this, () => f(stream()), name, requireModel)

  def from2Stream[C, D](s2: Source[D],
                        f: (Stream[A], Stream[D]) => Stream[C],
    name: String,
    requireModel: Boolean = false  ) =
    Op2.apply(this,
              s2,
              () => f(stream(), s2.stream()),
              name,
      requireModel)

  def filter(b: (A) => Boolean, name: String = ""): Source[A] =
    fromStream(_.filter(b), "Filter " + getStrOrElse(name, b.toString), RequireModel.isRequiring(b))

  def takeWhile(b: (A) => Boolean, name: String = ""): Source[A] =
    fromStream(_.takeWhile(b), "TakeWhile " + getStrOrElse(name, b.toString), RequireModel.isRequiring(b))

  def map[C](f: (A) => C, name: String = ""): Source[C] =
    fromStream(_.map(f), "Map " + getStrOrElse(name, f.toString), RequireModel.isRequiring(f))

  //Divide the frequency of the stream by n
  def divider(n: Int) =
    fromStream((s: Stream[A]) => s.grouped(n).map(_.last).toStream,
               "Divider " + n)

  def flatMap[C](f: (A) => Stream[C], name: String = ""): Source[C] =
    fromStream(_.flatMap(f), "FlatMap " + getStrOrElse(name, f.toString), RequireModel.isRequiring(f))

  def zip[C](s2: Source[C]) =
    from2Stream(s2, (s1s: Stream[A], s2s: Stream[C]) => s1s.zip(s2s), "Zip")

}

trait Source1[A] extends Sourcable {
  def source: Source[A]
  lazy val sources = List(source)
}

trait Source2[A, B] extends Sourcable {
  def source1: Source[A]
  def source2: Source[B]
  lazy val sources = List(source1, source2)
}

trait Source3[A, B, C] extends Sourcable {
  def source1: Source[A]
  def source2: Source[B]
  def source3: Source[C]
  lazy val sources = List(source1, source2, source3)
}

trait Op1[A, B] extends Source[B] with Source1[A]

object Op1 {
  def apply[A, B](source1: Source[A],
                  stream1: () => Stream[B],
                  name1: String,
                  requireModel1: Boolean = false) = new Op1[A, B] {
    override def toString     = name1
    def source                = source1
    def genStream()           = stream1()
    override def requireModel = requireModel1
  }
}

trait Op2[A, B, C] extends Source[C] with Source2[A, B]

object Op2 {
  def apply[A, B, C](source11: Source[A],
                     source12: Source[B],
                     stream1: () => Stream[C],
                     name1: String,
                     requireModel1: Boolean = false) = new Op2[A, B, C] {
    override def toString     = name1
    def source1               = source11
    def source2               = source12
    def genStream()           = stream1()
    override def requireModel = false
  }
}

trait Op3[A, B, C, D] extends Source[D] with Source3[A, B, C]

trait Resettable {
  def reset(): Unit
}

abstract class NamedFunction[A, B](f: A => B, name: String) extends (A => B) {
  override def toString = name
  def apply(x: A)       = f(x)
}
