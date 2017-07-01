package dawn.flow

import io.circe.generic.JsonCodec

@JsonCodec
case class Timestamped[A](t: Time, v: A, dt: Time = 0) {
  def time = t + dt
  def map[B](f: A => B) = Timestamped(t, f(v), dt)
  def addLatency(dt: Time): Timestamped[A] = copy(dt = this.dt + dt)
  override def toString = "TF["+time+"] "+ v
}

object Timestamped {
  def apply[A](x: A): Timestamped[A] = Timestamped(0, x)
}
