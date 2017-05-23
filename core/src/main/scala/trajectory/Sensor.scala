package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._

case class Accelerometer(cov: DenseMatrix[Real])(implicit val mc: ModelCallBack[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getFullLocalAcceleration(t)

}

object Accelerometer {
  def apply(source: Source[Time], cov: DenseMatrix[Real])(implicit mc: ModelCallBack[Trajectory]): SourceT[Acceleration] = source.map(Accelerometer(cov))
}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)(implicit val mc: ModelCallBack[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getBodyRates(t, dt)

}

object Gyroscope {
  def apply(source: Source[Time], cov: DenseMatrix[Real], dt: Timestep)(implicit mc: ModelCallBack[Trajectory]): SourceT[BodyRates] = source.map(Gyroscope(cov, dt))
}


case class Vicon(cov: DenseMatrix[Real])(implicit val mc: ModelCallBack[Trajectory]) extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) = 
    traj.getPosition(t)

}

object Vicon {
  def apply(source: Source[Time], cov: DenseMatrix[Real])(implicit mc: ModelCallBack[Trajectory]): SourceT[Position] = source.map(Vicon(cov))
}

case class ControlInput(std: Real)(implicit val mc: ModelCallBack[Trajectory]) extends Sensor[Thrust, Trajectory] {

   def generate(traj: Trajectory, t: Time) = 
     Rand.gaussian(traj.getThrust(t), std)

}

case class Sensor2[A, B, M](sensor1: Sensor[A, M], sensor2: Sensor[B, M])
    extends Sensor[(A, B), M] {
  def mc = sensor1.mc

  def generate(model: M, t: Time) =
    (sensor1.generate(model, t), sensor2.generate(model, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
  def apply(source: Source[Time], acc: Accelerometer, gyro: Gyroscope): SourceT[(Acceleration, BodyRates)] = source.map(Sensor2(acc, gyro))
}
