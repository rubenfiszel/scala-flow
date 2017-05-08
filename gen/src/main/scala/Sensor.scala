package spatial.fusion.gen

abstract class Sensor[+A] {

  //1% variance in time
  def timeVariance: Double = 0.01
  //used to set different seed
  def sensID: Int = getClass.getName.hashCode()

  def generate(traj: Trajectory,
               dt: Timestep,
               tf: Timeframe,
               seed: Seed): Stream[Timestamped[A]] =
    genTimes(dt, tf, timeVariance, seed + sensID).map(generatePoint(traj, _, seed + sensID))

  def generatePoint(traj: Trajectory, t: Time, seed: Seed): Timestamped[A]

}
