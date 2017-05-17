package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._

case class Accelerometer(cov: DenseMatrix[Real])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getFullLocalAcceleration(t)

}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getBodyRates(t, dt)

}

case class Vicon(cov: DenseMatrix[Real]) extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) = 
    traj.getPosition(t)

}

case class Sensor2[A, B, M](sensor1: Sensor[A, M], sensor2: Sensor[B, M])
    extends Sensor[(A, B), M] {

  def generate(model: M, t: Time) =
    (sensor1.generate(model, t), sensor2.generate(model, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
}
