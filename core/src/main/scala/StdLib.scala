package dawn.flow

import spire.math._
import spire.implicits._

case class LambdaWithModel[A, B, M](f: (A, M) => B, name: String = "")(implicit val mc: ModelCallBack[M]) extends (A => B) with RequireModel[M] {
  override def toString = getStrOrElse(name, "Lambda")
  def apply(x: A) = f(x, model.get)
}


object Clock {

  def genPerfectTimes(dt: Timestep): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0)
  }

}
case class Clock(dt: Timestep, tf: Timeframe)(implicit val sh: Scheduler) extends EmitterStream[Time] {

  def name = "Clock " + dt 
  def stream() = 
    Clock.genPerfectTimes(dt).takeWhile(_ < tf).map(x => (x, x))

}


//Case class and not function because A is a type parameter not known in advance
case class Integrate[A: Vec](source1: Source[A], dt: Timestep) extends Op1[A, A] {
  def name = "Integrate " + dt
  def listen1(x: A) =
    broadcast(x*dt)
}

case class Timestamp(scale: Real)  extends NamedFunction( (x: Time) => Timestamped(x, x*scale), "Timestamp")

trait Buffer[A] extends Op1[A, A] {
  def name = "Buffer"
  def init: A
  sh.executeAtStart(broadcast(init))
  def listen1(x: A) = 
    broadcast(x)
}

//Need companion object for call-by-name evaluation of source1
//Case classes don't support call-by-name
object Buffer {
  def apply[A](source11: => Source[A], init1: A, sh1: Scheduler) = new Buffer[A] {
    override def sh = sh1
    val init = init1
    def source1 = source11
  }
}

