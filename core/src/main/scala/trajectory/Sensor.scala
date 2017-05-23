package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._

case class Accelerometer(cov: DenseMatrix[Real])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getFullLocalAcceleration(t)

}

object Accelerometer {
  def apply(source: Source[Time, Trajectory], cov: DenseMatrix[Real]): SourceT[Acceleration, Trajectory] = source.map(Accelerometer(cov))
}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getBodyRates(t, dt)

}

object Gyroscope {
  def apply(source: Source[Time, Trajectory], cov: DenseMatrix[Real], dt: Timestep): SourceT[BodyRates, Trajectory] = source.map(Gyroscope(cov, dt))
}


case class Vicon(cov: DenseMatrix[Real]) extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) = 
    traj.getPosition(t)

}

object Vicon {
  def apply(source: Source[Time, Trajectory], cov: DenseMatrix[Real]): SourceT[Position, Trajectory] = source.map(Vicon(cov))
}

case class ControlInput(std: Real) extends Sensor[Thrust, Trajectory] {

   def generate(traj: Trajectory, t: Time) = 
     Rand.gaussian(traj.getThrust(t), std)

}

case class Sensor2[A, B, M](sensor1: Sensor[A, M], sensor2: Sensor[B, M])
    extends Sensor[(A, B), M] {

  def generate(model: M, t: Time) =
    (sensor1.generate(model, t), sensor2.generate(model, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
  def apply(source: Source[Time, Trajectory], acc: Accelerometer, gyro: Gyroscope): SourceT[(Acceleration, BodyRates), Trajectory] = source.map(Sensor2(acc, gyro))
}
