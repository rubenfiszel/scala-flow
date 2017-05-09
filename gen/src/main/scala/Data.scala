package spatial.fusion.gen

import breeze.stats.distributions._

trait Data[A] {
  def toValues(x: A): Seq[Real]
}

case class Timestamped[+A](t: Time, v: A)

trait Resettable {
  def reset(): Unit
}


trait Source[A] {
  def generate(): A
  def isEmpty: Boolean
  def gen() =
    if (isEmpty)
      None
    else
      Some(generate())
}

trait SourceStreamed[A] extends Source[A] with Resettable {
  def stream(): Stream[A]
  var iterator = stream().toIterator
  def isEmpty = !iterator.hasNext  
  def generate() = iterator.next()
  def reset() = {
    iterator = stream().toIterator
  }
}

/*case class Zip[A, B](sourceA: Source[A], sourceB: Source[B]) extends Source[(A,B)] {
  def generate() = (sourceA.generate(), sourceB.generate())
  def isEmpty =
    sourceA.isEmpty || sourceB.isEmpty
}
*/

trait Transformation[-A, +B] {
  def process(x:A): B
}


trait Transform[A, B] extends Source[B] {
  def transformation: Transformation[A, B]
  def source: Source[A]
  def generate(): B =
    transformation.process(source.generate())
  def isEmpty = source.isEmpty
}

trait TransformStreamed[A, B] extends SourceStreamed[B] with Transform[A, B] {
  def source: SourceStreamed[A]
  def stream() = source.stream().map(transformation.process)
  override def reset() =  {source.reset(); super.reset }
  override def generate(): B = iterator.next
  override def isEmpty = iterator.isEmpty
}

object Transform {

  def apply[A,B](s: SourceStreamed[A], t: Transformation[A,B]): TransformStreamed[A, B] =
    new TransformStreamed[A,B]() {
      lazy val transformation = t      
      lazy val source = s
    }

  def apply[A,B](s: Source[A], t: Transformation[A,B]): Transform[A, B] =
    new Transform[A,B]() {
      lazy val transformation = t
      lazy val source = s      
    }
  
}

trait Sink[A] extends Transformation[A, Unit] {
  def source: Source[A]
  def consumeAll() =
    while (!source.isEmpty)
      process(source.generate())
}


//trait Sink2[A,B](sink1: Sink[A], sink2: Sink[B]) {
//}

case class PrintSink[A](source: Source[A]) extends Sink[A] {
  def process(x: A) = println(x)
}

case class Clock(dt: Timestep) extends SourceStreamed[Time] {
  def stream() = genPerfectTimes(dt)
}

case class IdTrans[A]() extends Transformation[A, A] {
  def process(x: A) = x
}

case class ClockStop(source: SourceStreamed[Time], tf: Time) extends TransformStreamed[Time, Time] {
  lazy val transformation: Transformation[Time, Time] = IdTrans[Time]()
  override def stream() = source.stream().map(transformation.process).takeWhile(_ < tf)
}

case class ClockVar(std: Timestep, seed: Seed) extends Transformation[Time, Time]{
  val rb = RandBasis.withSeed(seed)
  def process(x: Time) = Gaussian(x, std)(rb).draw()
}
