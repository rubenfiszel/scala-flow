package dawn.flow

trait SignalFilter[A, B, C] extends ((C, Timestamped[A]) => Timestamped[B]) {

  def filter(p: C, x: Timestamped[A]): B

  def apply(p: C, x: Timestamped[A]) = Timestamped(x.t, filter(p, x))
}


case class ComplimentaryFilter(alpha: Real, dt: Timeframe)
    extends SignalFilter[(Acceleration, BodyRates), NormalVector, Trajectory] {

  var angle: Vec3 = Vec3(0, 0, 0)

  def filter(p: Trajectory, x: Timestamped[(Acceleration, BodyRates)]) = {
    val (acc, gyro) = x.v
    angle = Vec3(((angle + (gyro :* dt) :* alpha) + (acc :* alpha)).toArray)
    angle
  }

}
