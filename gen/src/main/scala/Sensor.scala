package spatial.fusion.gen

import breeze.linalg._

abstract class Sensor[+A] {

  //1% variance in time
  def timeVariance: Double = 0.01
  //used to set different seed
  def sensID: Int = getClass.getName.hashCode()

  def generate(traj: Trajectory,
               dt: Timestep,
               tf: Timeframe,
               seed: Seed): Stream[Timestamped[A]] =
    genTimes(dt, tf, timeVariance, seed + sensID)
      .map(t => {
        val nSeed = seed + sensID + t.hashCode
        generateDataWithTime(traj, t, nSeed)
      })

  def generateDataWithTime(traj: Trajectory,
                            t: Time,
                            seed: Seed): Timestamped[A] =
    Timestamped(t, generateData(traj, t, seed))

  def generateData(traj: Trajectory, t: Time, seed: Seed): A

}

case class Accelerometer(cov: DenseMatrix[Real]) extends Sensor[Acceleration] {

  def generateData(traj: Trajectory, t: Time, seed: Seed) = {
    traj.getAcceleration(t)
  }

}
