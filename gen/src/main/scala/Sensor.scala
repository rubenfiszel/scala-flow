package spatial.fusion.gen

import breeze.linalg._
import breeze.stats.distributions._

trait Sensor[A] {

  //1% variance in time
//  def timeVariance: Double = 0.01

  def generate(traj: Trajectory, t: Time): A

}

case class Accelerometer(cov: DenseMatrix[Real]) extends Sensor[Acceleration] {

  def generate(traj: Trajectory, t: Time) = {
    Vec3(
      MultivariateGaussian(traj.getAcceleration(t), cov)(Random)
        .draw()
        .toArray)
  }

}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)
    extends Sensor[BodyRates] {

  def generate(traj: Trajectory, t: Time) = {
    Vec3(
      MultivariateGaussian(traj.getBodyRates(t, dt), cov)(Random)
        .draw()
        .toArray)
  }

}

case class Vicon(cov: DenseMatrix[Real]) extends Sensor[Position] {

  def generate(traj: Trajectory, t: Time) = {
    Vec3(MultivariateGaussian(traj.getPosition(t), cov)(Random).draw().toArray)
  }

}

case class Sensor2[A, B](sensor1: Sensor[A], sensor2: Sensor[B]) extends Sensor[(A,B)]{
  def generate(traj: Trajectory, t: Time) =
    (sensor1.generate(traj, t), sensor2.generate(traj, t))
}

case class SensorPulse[A](source: Source[Time], traj: Trajectory, sensor: Sensor[A])
    extends Map[Time, Timestamped[A]] {
  def f(t: Time) =
    Timestamped(t, sensor.generate(traj, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
}
