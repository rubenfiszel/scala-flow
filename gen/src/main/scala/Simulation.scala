package spatial.fusion.gen


case class Simulation(traj: Trajectory, sensors: Seq[(Timestep, Sensor[_])]) {

  def simulate(dt: Timestep) = {
    val tf = traj.tf

    val keypoints = KeypointSource(traj)
    val clock = ClockStop(Clock(dt), tf)
    val points = Cache(TrajectoryPointPulse(clock, traj))
//    val datas = sensors.map { case (ts, s) => s.generate(traj, ts, tf, seed) }
//(keypoints, points, datas)
    (keypoints, points)
  }

}
