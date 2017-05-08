package spatial.fusion.gen

trait Data {
  def toValues: Seq[Real]
}

case class Timestamped[+A](t: Time, v: A)

case class Simulation(traj: Trajectory, sensors: Seq[(Timestep, Sensor[Data])]) {

  def simulate(dt: Timestep, seed: Seed): (Seq[Timestamped[Keypoint]], Stream[Timestamped[TrajectoryPoint]], Seq[Stream[Timestamped[Data]]]) = {
    val tf = traj.tf

    var ts = 0.0
    val keypoints = traj.keypoints.map { case (kp, tf) => {ts += tf; Timestamped(ts, kp) } }
    val points = genPerfectTimes(dt, tf).map(t => Timestamped(t, traj.getPoint(t)))
    val datas = sensors.map { case (ts, s) => s.generate(traj, ts, tf, seed) }
    (keypoints, points, datas)
  }

}
