package dawn.flow

import breeze.linalg._
import breeze.stats.distributions._

trait Sensor[A, M] extends ((M, Time) => Timestamped[A]) {

  override def toString = getClass.getSimpleName
  
  def generate(model: M, t: Time): A

  def apply(p: M, t: Time) =
    Timestamped(t, generate(p, t))
}

case class Accelerometer(cov: DenseMatrix[Real]) extends Sensor[Acceleration, Trajectory] {

  def generate(traj: Trajectory, t: Time) = {
    Vec3(
      MultivariateGaussian(traj.getAcceleration(t), cov)(Random)
        .draw()
        .toArray)
  }

}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)
    extends Sensor[BodyRates, Trajectory] {

  def generate(traj: Trajectory, t: Time) = {
    Vec3(
      MultivariateGaussian(traj.getBodyRates(t, dt), cov)(Random)
        .draw()
        .toArray)
  }

}

case class Vicon(cov: DenseMatrix[Real]) extends Sensor[Position, Trajectory] {

  def generate(traj: Trajectory, t: Time) = {
    Vec3(MultivariateGaussian(traj.getPosition(t), cov)(Random).draw().toArray)
  }

}

case class Sensor2[A, B, M](sensor1: Sensor[A, M], sensor2: Sensor[B, M]) extends Sensor[(A,B), M]{
  def generate(model: M, t: Time) =
    (sensor1.generate(model, t), sensor2.generate(model, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
}
