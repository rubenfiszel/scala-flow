package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object ParticleFilterOutdoor extends FlowApp[Trajectory, TrajInit] {

  //****** Model ******

  val dtIMU  = 0.05
  val dtAlt  = (dtIMU * 10) + dtIMU * 2
  val dtGPS1 = (dtIMU * 500) + dtIMU * 10
  val dtGPS2 = (dtIMU * 500) + dtIMU * 310

  val covScale = 0.1
  //filter parameter
  val covAcc  = 1.0 * covScale
  val covGyro = 1.0 * covScale
  val covMag  = 1.0 * covScale
  val varAlt  = 1.0 * covScale
  val covGPS1 = 0.1 * covScale
  val covGPS2 = 0.1 * covScale

  val clockIMU  = new TrajectoryClock(dtIMU)
  val clockAlt  = new TrajectoryClock(dtAlt)
  val clockGPS1 = new TrajectoryClock(dtGPS1)
  val clockGPS2 = new TrajectoryClock(dtGPS2)

  val imu  = clockIMU.map(IMU(eye(3) * covAcc, eye(3) * covGyro, dtIMU))
  val mag  = clockIMU.map(Magnetometer(eye(3) * covMag))
  val gps1 = clockGPS1.map(GPS(eye(3) * covGPS1))
  val gps2 = clockGPS2.map(GPS(eye(3) * covGPS2))
  val alt  = clockAlt.map(Altimeter(varAlt))

  val numberParticles = 200

  val filter = ParticleFilterMag2GPSAlt(
    imu,
    mag,
    gps1,
    gps2,
    alt,
    numberParticles,
    covAcc,
    covGyro,
    covMag,
    covGPS1,
    covGPS2,
    varAlt
  )

  val testLog = new TestLogger()

  def figure() = {
    val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")
    val pqs =
      points
        .map(x => (x.p, x.q), "toPandQ")

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

  val trajs = TrajFactory.generate(1)
  trajs.foreach(traj => {
    run(traj, traj.trajInit)
  })

  testLog.printAll()
  testLog.printMean()

  System.exit(0)

}
