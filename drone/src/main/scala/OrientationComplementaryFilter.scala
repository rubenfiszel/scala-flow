package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg.norm
import spire.math.Quaternion

object OCF extends FlowApp[Trajectory, TrajInit] {

  //****** Model ******

  val dtIMU   = 0.005

  val covScale = 0.1
  //filter parameter
  val covAcc    = 1.0 * covScale
  val covGyro   = 10.0 * covScale

  val clockIMU   = new TrajectoryClock(dtIMU)

  val imu   = clockIMU.map(IMU(eye(3) * covAcc, eye(3) * covGyro, dtIMU))
  val thrust = clockIMU.map(ControlInputThrust(covAcc, dtIMU))

  val cfilter = OrientationComplementaryFilter(imu, thrust, 0.98)
  //val attitude = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getOrientationQuaternion(t)), "toQuat")
  val distance = cfilter.mapT(LambdaWithModel((ts: Timestamped[Quat], traj: Trajectory) =>
    (Quat.distance(traj.getOrientationQuaternion(ts.t), ts.v), 0.0)))

  val (dis, zero) = distance.unzip2

  Plot(dis)

  val labeled = dis.labelData(_ => (0.0))
  TestTS(labeled, "cfilter")


  drawExpandedGraph()

  val trajs = TrajFactory.generate(1)

  trajs.foreach(traj => {
    run(traj, traj.trajInit)
  })

  System.exit(0)

//  /*
  //  /* batch example
  val (accelerometer, _) = imu.unzip2
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2.0), "*2")

  Plot(batch, accelerometer)  

  

}
