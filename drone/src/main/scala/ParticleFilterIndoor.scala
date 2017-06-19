package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object ParticleFilterIndoor extends FlowApp[Trajectory, TrajInit] {

  //****** Model ******

  val dtIMU   = 0.005
  val dtVicon = (dtIMU * 50)

  val covScale = 0.8
  //filter parameter
  val covAcc    = 1.0 * covScale
  val covGyro   = 1.0 * covScale
  val covViconP = 0.1 * covScale
  val covViconQ = 0.1 * covScale

  val numberParticles = 1200

  val clockIMU   = new TrajectoryClock(dtIMU)
  val clockVicon = new TrajectoryClock(dtVicon)

  val imu   = clockIMU.map(IMU(eye(3) * covAcc, eye(3) * covGyro, dtIMU))
  val vicon = clockVicon.map(Vicon(eye(3) * covViconP, eye(3) * covViconQ))

  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")
  Plot2(batch, accelerometer)
   */

  val filterN = 2

  val filter = filterN match {
    case 0 =>
      AugmentedComplementaryFilter(
        imu,
        vicon,
        0.95
      )
    case 1 =>
      ParticleFilterVicon(
        imu,
        vicon,
        numberParticles,
        covAcc,
        covGyro,
        covViconP,
        covViconQ
      )
    case 2 =>
      EKFVicon(
        imu,
        vicon,
        numberParticles,
        covAcc,
        covGyro,
        covViconP,
        covViconQ
      )
  }

  val testLog = new TestLogger()


  def figure() = {
    val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

    val pqs =
      points.map(x => (x.p, x.q), "toPandQ")

    Plot2(filter, pqs)
  }

  def testTS() = {
    val toDist = filter.mapT(
      LambdaWithModel((ts: Timestamped[(Position, Quat)], traj: Trajectory) =>
        (norm(traj.getPosition(ts.t) - ts.v._1), Quat.distance(traj.getOrientationQuaternion(ts.t), ts.v._2))),
      "DistanceToLabel"
    )

    val labeled = toDist.labelData(_ => (0.0, 0.0))
    TestTS(labeled, Some(testLog))
  }

  figure()
  testTS()
//  filter.println

//  drawExpandedGraph()
  val trajs = TrajFactory.generate(1)
  trajs.foreach(traj => {
    run(traj, traj.trajInit)
  })

  testLog.printAll()
  testLog.printMean()

  System.exit(0)

}
