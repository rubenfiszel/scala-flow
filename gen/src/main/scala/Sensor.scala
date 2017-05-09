package spatial.fusion.gen

import breeze.linalg._
import breeze.stats.distributions._

trait Sensor[A] {

  //1% variance in time
//  def timeVariance: Double = 0.01

 
  def generate(traj: Trajectory, t: Time, seed: Seed): A

}

case class Accelerometer(cov: DenseMatrix[Real]) extends Sensor[Acceleration] {

  def generate(traj: Trajectory, t: Time, seed: Seed) = {
    Vec3(
      MultivariateGaussian(traj.getAcceleration(t), cov)(
        RandBasis.withSeed(seed)).draw().toArray)
  }

}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)
    extends Sensor[BodyRates] {

  def generate(traj: Trajectory, t: Time, seed: Seed) = {
    Vec3(
      MultivariateGaussian(traj.getBodyRates(t, dt), cov)(
        RandBasis.withSeed(seed)).draw().toArray)
  }

}

case class Vicon(cov: DenseMatrix[Real])
    extends Sensor[Position] {

  def generate(traj: Trajectory, t: Time, seed: Seed) = {
    Vec3(
      MultivariateGaussian(traj.getPosition(t), cov)(
        RandBasis.withSeed(seed)).draw().toArray)
  }

}

case class SensorTransformation[A](traj: Trajectory, sensor: Sensor[A], seed: Seed) extends Transformation[Time, A] {
  def process(t: Time) = sensor.generate(traj, t, seed + t.hashCode() + sensor.hashCode())
}
