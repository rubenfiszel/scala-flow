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

case class PositionSensor(cov: MatrixR)(implicit val modelHook: ModelHook[Trajectory])
    extends VectorSensor[Trajectory] {

  def genVector(traj: Trajectory, t: Time) =
    traj.getPosition(t)
    
}

case class AttitudeSensor(cov: MatrixR)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[Quat, Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    Quat.genQuaternion(traj.getOrientationQuaternion(t), cov)

}


case class Altimeter(variance: Real)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[Real, Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    Rand.gaussian(traj.getPosition(t).z, variance)

}



case class ControlInputThrust(variance: Real, dt: Timestep)(implicit val modelHook: ModelHook[Trajectory])
    extends Sensor[Thrust, Trajectory] {

  def generate(traj: Trajectory, t: Time) =
    Rand.gaussian(traj.getThrust(t), variance)

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
  def apply(covAcc: MatrixR, covGyro: MatrixR, dtGyro: Time)(implicit mh: ModelHook[Trajectory]) =
    Sensor2(Accelerometer(covAcc), Gyroscope(covGyro, dtGyro))
}

object GPS {

  def apply(cov: MatrixR)(implicit modelHook: ModelHook[Trajectory]) =
    PositionSensor(cov)

}

object Vicon {

  def apply(covP: MatrixR, covQ: MatrixR)(implicit modelHook: ModelHook[Trajectory]) =
    Sensor2(PositionSensor(covP), AttitudeSensor(covQ))

}
