package spatial.fusion.gen

trait Data {
  def toValues: Seq[Real]
}

case class Timestamped[+A](t: Time, v: A)

case class Simulation(traj: Trajectory, sensors: Seq[(Timestep, Sensor[Data])]) {

  def simulate(dt: Timestep, seed: Seed): (Stream[Timestamped[TrajectoryPoint]], Seq[Stream[Timestamped[Data]]]) = {
    val tf = traj.tf
    val points = genPerfectTimes(dt, tf).map(t => Timestamped(t, traj.getPoint(t)))
    val datas = sensors.map { case (ts, s) => s.generate(traj, ts, tf, seed) }
    (points, datas)
  }

}
