package dawn.flow.trajectory

import dawn.flow.spatial._
import dawn.flow._
import breeze.linalg.norm
import spire.math.Quaternion

object ParticleFilterIndoor extends FlowApp[Trajectory, TrajInit] {

  //****** Model ******

  val dtIMU   = 0.005
  val dtVicon = (dtIMU * 50)

  val covScale = 0.1
  //filter parameter
  val covAcc    = 1.0 * covScale
  val covGyro   = 10.0 * covScale
  val covViconP = 0.1 * covScale
  val covViconQ = 0.1 * covScale

  val numberParticles = 1200

  val clockIMU   = new TrajectoryClock(dtIMU).stop(0.3)
  val clockVicon = new TrajectoryClock(dtVicon).stop(0.3)

  val imu   = clockIMU.map(IMU(eye(3) * covAcc, eye(3) * covGyro, dtIMU))
  val vicon = clockVicon.map(Vicon(eye(3) * covViconP, eye(3) * covViconQ))


  lazy val acfilter = 
      AugmentedComplementaryFilter(
        imu,
        vicon,
        0.95
      )

  lazy val pfilter = 
      ParticleFilterVicon(
        imu,
        vicon,
        numberParticles,
        covAcc,
        covGyro,
        covViconP,
        covViconQ
      )

  lazy val ekfilter = 
      EKFVicon(
        imu,
        vicon,
        covAcc,
        covGyro,
        covViconP,
        covViconQ
      )

  lazy val ukfilter =   
      UKFVicon(
        imu,
        vicon,
        covAcc,
        covGyro,
        covViconP,
        covViconQ
      )

  lazy val sfilter =
    PFSpatial(
      imu,
      vicon)

  def filter = ekfilter


  val filters = List(acfilter, ekfilter, ukfilter, pfilter, sfilter)

  val testLogs = filters.map(x => new TestLogger(x.toString.take(10)))

  def figure() = {
    val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

    val pqs =
      points.map(x => (x.p, x.q), "toPandQ")

    Plot(pqs, filters:_*)
  }

  def figureError() = {


    val toDists = filters.map(_.mapT(
      LambdaWithModel((ts: Timestamped[(Position, Quat)], traj: Trajectory) =>
        (norm(traj.getPosition(ts.t) - ts.v._1), Quat.distance(traj.getOrientationQuaternion(ts.t), ts.v._2))),
      "DistanceToLabel"
    ))
    
    Plot(toDists(0), toDists.drop(1):_*)
  }
  
  def testTS(s: Source[(Position, Quat)], tl: TestLogger) = {

    val toDist = s.mapT(
      LambdaWithModel((ts: Timestamped[(Position, Quat)], traj: Trajectory) =>
        (norm(traj.getPosition(ts.t) - ts.v._1), Quat.distance(traj.getOrientationQuaternion(ts.t), ts.v._2))),
      "DistanceToLabel"
    )
    
    val labeled = toDist.labelData(_ => (0.0, 0.0))
    TestTS(labeled, s.toString.take(10), Some(tl) )
  }

  filters.zip(testLogs).foreach { case (x,y) => testTS(x, y) }

  figure()

  drawExpandedGraph()

  val trajs = TrajFactory.generate(1)

//  new Jzy3dVisualisation(trajs(0))
  trajs.foreach(traj => {
    run(traj, traj.trajInit)
  })

  testLogs.foreach(_.printAll)
  testLogs.foreach(_.printMean)

  System.exit(0)

}
