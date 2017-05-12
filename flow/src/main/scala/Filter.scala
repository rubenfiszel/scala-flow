package dawn.flow

trait Filter[A, B, C] {

  def filter(p: C, x: Timestamped[A]): Timestamped[B]

}

case class FilterApply[A, B, C](source: SourceT[A, C], filter: Filter[A, B, C])
    extends MapT[A, B, C] {

  def f(p: C, x: Timestamped[A]) = filter.filter(p, x)
}

case class ComplimentaryFilter(alpha: Real, dt: Timeframe)
    extends Filter[(Acceleration, BodyRates), NormalVector, Trajectory] {

  var angle: Vec3 = Vec3(0, 0, 0)

  def filter(p: Trajectory, x: Timestamped[(Acceleration, BodyRates)]) = {
    val (acc, gyro) = x.v
    angle = Vec3(((angle + (gyro :* dt) :* alpha) + (acc :* alpha)).toArray)
    Timestamped(x.t, angle)
  }

}
