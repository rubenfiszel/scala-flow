package dawn.flow

sealed trait Sink

trait Sink1[A] extends Node with Source1[A] with Sink {
  def f(x: Timestamped[A]): Unit
  def listen1(x: Timestamped[A]) =
    f(x)
}

trait SinkBatchN[A]
    extends Node
    with CloseListener
    with Accumulate[A]
    with Sink {

  def schedulerClose = scheduler

  def onScheduleClose() = {
    accumulated = accumulated.map(_.reverse)
    consumeAll(accumulated)
  }

  def consumeAll(x: Array[ListT[A]]): Unit
}

trait SinkBatch1[A] extends SinkBatchN[A] with Source1[A] {

  def listen1(x: Timestamped[A]) = ()

  def consumeAll(x: Array[ListT[A]]) = {
    consumeAll1(x(0))
  }
  def consumeAll1(x: ListT[A]): Unit
}

/*
trait SinkP extends Sink {
  def isEmpty: Boolean
  def consume(): Unit
  def consumeAll() =
    while (!isEmpty) consume()
}

trait Sink1[A] extends SinkP with Source1[A] {

  def f(x: A): Unit

  var iterator: Option[Iterator[A]] = None

  def isEmpty = iterator.isDefined && !iterator.get.hasNext

  def consume() = {
    if (!iterator.isDefined)
      iterator = Some(source.stream().toIterator)

    if (iterator.get.hasNext) {
      val n = iterator.get.next
      f(n)
    }

  }
}

trait Sink2[A, B] extends SinkP with Source2[A, B] {

  def f1(x: A): Unit
  def f2(x: B): Unit

  val s1 = new Sink1[A] {
    def source        = source1
    def f(x: A) = f1(x)
  }

  val s2 = new Sink1[B] {
    def source        = source2
    def f(x: B) = f2(x)
  }

  def isEmpty = s1.isEmpty && s2.isEmpty

  def consume() = {
    s1.consume()
    s2.consume()
  }
}


trait SinkTimestamped[A] extends Sink {
  this: Node =>

  def isEmpty: Boolean
  def consume(): Unit =
    ???

  def next: Unit
  def current: Option[Timestamped[A]]
  def f(x: Timestamped[A]): Unit

}
 */
