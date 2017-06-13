package dawn.flow

import spire.math._
import spire.implicits._

case class LambdaWithModel[A, B, M](f: (A, M) => B, name: String = "")(
    implicit val modelHook: ModelHook[M])
    extends (A => B)
    with RequireModel[M] {
  override def toString = getStrOrElse(name, "Lambda")
  def apply(x: A) = f(x, model.get)
}

object Clock {

  def genPerfectTimes(dt: Timestep): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0)
  }

}

class Clock(dt: Timestep)(implicit val schedulerHook: SchedulerHook,
                               val nodeHook: NodeHook)
    extends EmitterStream[Time] {

  def name = "Clock " + dt
  def stream() =
    Clock.genPerfectTimes(dt).map(x => (x, x))

}

//Case class and not function because A is a type parameter not known in advance
case class Integrate[A: Vec](dt: Timestep)
    extends NamedFunction((x: A) => x * dt, "Integrate")

object Integrate {
  def apply[A: Vec](source: Source[A], dt: Time): Source[A] =
    source.map(Integrate(dt))
}

case class Timestamp(scale: Real)
    extends NamedFunction((x: Time) => x * scale, "Timestamp")

trait Buffer[A] extends Op1[A, A] {
  def name = "Buffer"
  def init: A
  def listen1(x: Timestamped[A]) =
    broadcast(x)

  //-1 to make sure we send before Timestamp 0
  def bcInit() =
    scheduler.executeIn(broadcast(Timestamped(init)), -1)

  bcInit()

  override def setup() = {
    super.setup()
    bcInit()
  }
}

//Need companion object for call-by-name evaluation of source1
//Case classes don't support call-by-name
//parent is because we need to have a reliable model and scheduler
//that doesn't rely on a loop.
object Buffer {
  def apply[A](rawSource11: => Source[A], init1: A, parent: Node) =
    new Buffer[A] {
      override def scheduler = parent.scheduler
      override def nodeHook = parent.nodeHook
      val init = init1
      def rawSource1 = rawSource11
    }
}
