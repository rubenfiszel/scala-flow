package dawn.flow

trait Sink1[A] extends Sourcable with Source1[A] {
  def f(x: A): Unit
  def listen1(x: A) = f(x)
}

trait Accumulate1[A] {
  var accumulated1 = List[A]()
  def listen1(x: A) =
    accumulated1 ::= x
}

trait Accumulate2[A, B] extends Accumulate1[A] {
  var accumulated2 = List[B]()  
  def listen2(x: B) =
    accumulated2 ::= x
  
}

trait SinkBatch1[A] extends Sourcable with CloseListener with Accumulate1[A] with Source1[A] {

  def shClose = sh

  def close() =
    consumeAll(accumulated1)

  def consumeAll(x: List[A]): Unit
}

trait SinkBatch2[A, B] extends Sourcable with CloseListener with Accumulate2[A, B] with Source2[A, B] {

  def shClose = sh

  def close() =
    consumeAll(accumulated1, accumulated2)

  def consumeAll(x: List[A], y: List[B]): Unit
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
  this: Sourcable =>

  def isEmpty: Boolean
  def consume(): Unit =
    ???

  def next: Unit
  def current: Option[Timestamped[A]]
  def f(x: Timestamped[A]): Unit

}
*/
