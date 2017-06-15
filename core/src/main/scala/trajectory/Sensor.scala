package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._

case class Accelerometer(cov: MatrixR)(implicit val modelHook: ModelHook[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getLocalAcceleration(t)

}

case class Gyroscope(cov: MatrixR, dt: Timestep)(implicit val modelHook: ModelHook[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getOmega(t, dt)

}

case class Vicon(covP: MatrixR, covQ: MatrixR)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[(Position, Quat), Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    (Rand.gaussian(traj.getPosition(t), covP), Quat.genQuaternion(traj.getOrientationQuaternion(t), covQ))

}

case class ControlInputThrust(std: Real, dt: Timestep)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[Thrust, Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    Rand.gaussian(traj.getThrust(t), std)

}

case class ControlInputOmega(covBR: MatrixR, dt: Timestep)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[Omega, Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    Rand.gaussian(traj.getOmega(t, dt), covBR)

}

case class Sensor2[A, B, M](sensor1: Sensor[A, M], sensor2: Sensor[B, M]) extends Sensor[(A, B), M] {
  def modelHook = sensor1.modelHook

  def generate(model: M, t: Time) =
    (sensor1.generate(model, t), sensor2.generate(model, t))
}

object IMU {
  def apply(acc: Accelerometer, gyro: Gyroscope) = Sensor2(acc, gyro)
  def apply(source: Source[Time], covAcc: MatrixR, covGyro: MatrixR, dtGyro: Time)(implicit mh: ModelHook[Trajectory]): Source[(Acceleration, Omega)] =
    source.map(Sensor2(Accelerometer(covAcc), Gyroscope(covGyro, dtGyro)))
}
