package dawn.flow

import spire.math._
import spire.implicits._

class StdLibSource[A, B](source: Source[A, B]) {

  def cache() = Cache(source)

  def buffer(init: A) = Buffer(source, init)
}

case class Clock(dt: Timestep) extends Source[Time, Null]  {
  override def toString = "Clock " + dt 
  def sources = List()
  def genStream(p: Null) = genPerfectTimes(dt)

  def genPerfectTimes(dt: Timestep): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0)
  }
  
}

case class Integrate[A: Vec, B](source: Source[A, B], dt: Timestep) extends Op1[A, B, A] {
  def genStream(p: B) = source.map((x: A) => x*dt).stream(p)
}

case class Timestamp(scale: Real)  extends NamedFunction1( (x: Time) => Timestamped(x, x*scale, 0), "Timestamp")

trait Buffer[A,B] extends Op1[A, B, A]  {

//  lazy val source: Source[A,B] = source1()
//  override lazy val sources = List(source)
  override def toString = "Buffer"
  def init: A
  def genStream(p: B) = init #:: source.stream(p)

}

//Need companion object for call-by-name evaluation of source1
//Case classes don't support call-by-name
object Buffer {
  def apply[A, B](source1: => Source[A,B], init1: A) = new Buffer[A,B] {
    val init = init1
    def source = source1
  }
}

case class Cache[A, B](source: Source[A, B]) extends Op1[A, B, A] {

  def genStream(param: B) = source.stream(param)

  override def stream(param: B) = {
    if (!lastB.exists(_ == param)) {
      cStream = Some(genStream(param))
      lastB = Some(param)
    }
    cStream.get
  }
  
}
