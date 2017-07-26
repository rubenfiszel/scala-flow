package dawn.flow.trajectory

import dawn.flow.spatial._
import dawn.flow._
import breeze.linalg.{norm, DenseVector}
import spire.math.Quaternion

object MiniFilterIndoor extends FlowApp[Trajectory, TrajInit] {

  //****** Model ******

  val dtIMU   = 0.1
  val dtVicon = (dtIMU * 5)

  val N = 100
  //filter parameter
  val covAcc2d    = 0.1
  val covGPS = 0.001

  val clockIMU   = new TrajectoryClock(dtIMU).stop(dtIMU*101)
  val clockGPS = new TrajectoryClock(dtVicon).stop(dtIMU*101)

  /* ****** Gen  mini particle filter data **** */

  val points2d = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => {val p = traj.getPoint(t); Vec2(p.p(0), p.p(1))}), "toPoints")
  val vel2d = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => {val p = traj.getPoint(t); p.v(0)}), "toVel0")
  val acc2d = clockIMU.map(Accelerometer2D(eye(2) * covAcc2d))
  val gps2d = clockGPS
    .map(PositionSensor(eye(3) * covGPS))
    .map(x => Vec2(x(0), x(1)))

  val mini = SpatialMiniParticleFilter(acc2d, gps2d, dtIMU, N, covAcc2d, covGPS)


//  acc2d.map(_(0)).debug
//  vel2d.debug
//  points2d.debug
//  mini.debug

  Plot(points2d, mini)
  
  drawExpandedGraph()

  val trajs = TrajFactory.generate(1)

  trajs.foreach(traj => {
    run(traj, traj.trajInit)
  })
  

  System.exit(0)

}

