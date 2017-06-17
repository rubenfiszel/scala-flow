package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object ParticleFilterIndoor extends FlowApp[Trajectory] {

  //****** Model ******

  val dtIMU   = 0.001
  val dtCI    = dtIMU
  val dtVicon = (dtIMU * 500)
//  val dtAlt   = (dtIMU * 10) + dtIMU * 2
//  val dtGPS1  = (dtIMU * 500) + dtIMU * 10
//  val dtGPS2  = (dtIMU * 500) + dtIMU * 310

  val covScale = 10.0
  //filter parameter
  val covAcc    = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covGyro   = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covViconP = DenseMatrix.eye[Real](3) * (0.001 * covScale)
  val covViconQ = DenseMatrix.eye[Real](3) * (0.001 * covScale)
//  val varAlt    = 0.1 * covScale
//  val covGPS1   = DenseMatrix.eye[Real](3) * (0.1 * covScale)
//  val covGPS2   = DenseMatrix.eye[Real](3) * (0.1 * covScale)

//  val varianceCIThrust = 0.1 * covScale
//  val covCIOmega  = DenseMatrix.eye[Real](3) * (0.1 * covScale)

  val numberParticles = 200

  val initQ = Quaternion(1.0, 0, 0, 0)

  val clockIMU   = new TrajectoryClock(dtIMU)
  val clockCI    = new TrajectoryClock(dtCI).latency(dtIMU / 100.0)
  val clockVicon = new TrajectoryClock(dtVicon)
//  val clockAlt   = new TrajectoryClock(dtAlt)
//  val clockGPS1  = new TrajectoryClock(dtGPS1)
//  val clockGPS2  = new TrajectoryClock(dtGPS2)

  val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

  val imu   = clockIMU.map(IMU(covAcc, covGyro, dtIMU))
  val vicon = clockVicon.map(Vicon(covViconP, covViconQ))
//  val alt   = clockAlt.map(Altimeter(varAlt))
//  val gps1  = clockGPS1.map(GPS(covGPS1))
//  val gps2  = clockGPS2.map(GPS(covGPS2))
//  val controlInputThrust = clockCI.map(ControlInputThrust(varianceCIThrust, dtCI))
//  val controlInputOmega  = clockCI.map(ControlInputOmega(covCIOmega, dtCI))

  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")
  Plot2(batch, accelerometer)
   */

  val particleFilter = true

  val filter =
    if (!particleFilter)
      AugmentedComplementaryFilter(
        imu,
        vicon,
        initQ,
        0.95
      )
    else
      ParticleFilterVicon(
        imu,
        vicon,
        initQ,
        numberParticles,
        covAcc,
        covGyro,
        covViconP,
        covViconQ
      )

  val pqs =
    points
      .map(x => (x.p, x.q), "toPandQ")

  def awt() = {
    new Jzy3dVisualisation(points, traj.getKeypoints)
  }

  def figure() = {
    Plot2(filter, pqs)
  }

  def testTS() = {
    TestTS(filter, pqs, 1000)
  }

  val traj = TrajFactory.generate()

  figure()
  testTS()
//  awt()
//  filter.println

  run(traj)
  //  drawExpandedGraph()
  System.exit(0)

}
