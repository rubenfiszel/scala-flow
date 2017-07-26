package dawn.flow.trajectory

import dawn.flow.spatial._
import dawn.flow._
import breeze.linalg.{norm, DenseVector}
import spire.math.Quaternion

object ParticleFilterIndoor extends FlowApp[Trajectory, TrajInit] {

  //****** Model ******

  val dtIMU   = 0.005
  val dtVicon = (dtIMU * 5)

  val covScale = 0.1
  //filter parameter
  val covAcc    = 1.0 * covScale
  val covAcc2d    = 1.0 * covScale  
  val covGyro   = 10.0 * covScale
  val covViconP = 0.1 * covScale
  val covViconQ = 0.1 * covScale

  val numberParticles = 1200

  val clockIMU   = new TrajectoryClock(dtIMU).stop(dtIMU*200)
  val clockVicon = new TrajectoryClock(dtVicon).stop(dtIMU*200)

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


  val filters = List(acfilter, ekfilter, ukfilter, pfilter)//, sfilter)

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

  def f[A](x: ListT[A], pre: String) = {
    def g(x: Any): String = x match {
      case Timestamped(t, v, _) => pre+"(" + t + ", " + g(v) + "),"
      case x: Quat => s"SQuat(${x.r}, ${x.i}, ${x.j}, ${x.k})"        
      case x: DenseVector[Real] => "SVec3(" + x.toArray.map(_.toString).mkString(", ")+")"
      case (a, b) => g(a) + ", " + g(b)
      case _ => x.toString
    }
    x.map(g).foreach(println)
    x
  }

  /*
  /* ****** Gen rao black particle filter data **** */
  val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => {val p = traj.getPoint(t); (p.p, p.q)}), "toPoints")

  Batch(imu, (x: ListT[(Acceleration, Omega)]) => f(x, "SIMU"))
  Batch(vicon, (x: ListT[(Position, Attitude)]) => f(x, "SVicon")  )
  Batch(points, (x: ListT[(Position, Attitude)]) => f(x, "Points")  )
   */

  drawExpandedGraph()

  val trajs = TrajFactory.generate(1)

  trajs.foreach(traj => {
    run(traj, traj.trajInit)
  })

//  new Jzy3dVisualisation(trajs(0)).run()  

  
//  testLogs.foreach(_.printAll)
//  testLogs.foreach(_.printMean)

  System.exit(0)

}
