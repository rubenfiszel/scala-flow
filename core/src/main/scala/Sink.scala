package dawn.flow


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


trait SinkTimestamped[A, B] extends Sink[B] {
  this: Sourcable =>

  def isEmpty: Boolean
  def consume(p: B): Unit =
    ???

  def next: Unit
  def current: Option[Timestamped[A]]
  def f(x: Timestamped[A]): Unit

}
