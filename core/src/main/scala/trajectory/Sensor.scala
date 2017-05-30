package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._

case class Accelerometer(cov: DenseMatrix[Real])(
    implicit val modelHook: ModelHook[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getFullLocalAcceleration(t)

}

case class Gyroscope(cov: DenseMatrix[Real], dt: Timestep)(
    implicit val modelHook: ModelHook[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getBodyRates(t, dt)

}

case class Vicon(covP: MatrixR, covQ: MatrixR)(
    implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[(Position, Quat), Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    (Rand.gaussian(traj.getPosition(t), covP), Rand.gaussian(traj.getOrientationQuaternion(t).toDenseVector, covQ).toQuaternion)

}

case class ControlInput(std: Real, covBR: MatrixR, dt: Timestep)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[(Thrust, BodyRates), Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    (Rand.gaussian(traj.getThrust(t), std), Rand.gaussian(traj.getBodyRates(t, dt), covBR))

}

case class Sensor2[A, B, M](sensor1: Sensor[A, M], sensor2: Sensor[B, M])
    extends Sensor[(A, B), M] {
  def modelHook = sensor1.modelHook

  def generate(model: M, t: Time) =
    (sensor1.generate(model, t), sensor2.generate(model, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
  def apply(source: Source[Time],
            acc: Accelerometer,
            gyro: Gyroscope): Source[(Acceleration, BodyRates)] =
    source.map(Sensor2(acc, gyro))
}
