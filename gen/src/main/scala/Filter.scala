package spatial.fusion.gen

case class ComplimentaryFilter(source: Source[Timestamped[(Acceleration, BodyRates)], Trajectory], alpha: Real, dt: Timeframe) extends Map[Timestamped[(Acceleration, BodyRates)], Timestamped[NormalVector], Trajectory]{

  var angle: Vec3 = Vec3(0, 0, 0)

  def f(p: Trajectory, x: Timestamped[(Acceleration, BodyRates)]) = {
    val (acc, gyro) = x.v
    angle = Vec3(((angle + (gyro :* dt) :* alpha) + (acc :* alpha)).toArray)
    Timestamped(x.t, angle)
  }
  
}
