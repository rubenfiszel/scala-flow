package dawn.flow

import spire.math._
import spire.implicits._

case class LambdaWithModel[A, B, M](f: (A, M) => B, name: String = "")(implicit val mc: ModelCallBack[M]) extends (A => B) with RequireModel[M] {
  override def toString = getStrOrElse(name, "Lambda")
  def apply(x: A) = f(x, model.get)
}


class StdLibSource[A](source: Source[A]) {

  def cache() = Cache(source)

  def buffer(init: A) = Buffer(source, init)
}

case class Clock(dt: Timestep) extends Source[Time]  {
  override def toString = "Clock " + dt 
  def sources = List()
  def genStream() = genPerfectTimes(dt)

  def genPerfectTimes(dt: Timestep): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0)
  }
  

}


//Case class and not function because A is a type parameter not known in advance
case class Integrate[A: Vec](source: Source[A], dt: Timestep) extends Op1[A, A] {
  def genStream() = source.map((x: A) => x*dt).stream()
}

case class Timestamp(scale: Real)  extends NamedFunction( (x: Time) => Timestamped(x, x*scale, 0), "Timestamp")

trait Buffer[A] extends Op1[A, A]  {

//  lazy val source: Source[A,B] = source1()
//  override lazy val sources = List(source)
  override def toString = "Buffer"
  def init: A
  def genStream() = init #:: source.stream()

}

//Need companion object for call-by-name evaluation of source1
//Case classes don't support call-by-name
object Buffer {
  def apply[A](source1: => Source[A], init1: A) = new Buffer[A] {
    val init = init1
    def source = source1
  }
}

case class Cache[A](source: Source[A]) extends Op1[A, A] {

  def genStream() = source.stream()

  override def stream() = {
    if (!cStream.isDefined) {
      cStream = Some(genStream())
    }
    cStream.get
  }
  
}
