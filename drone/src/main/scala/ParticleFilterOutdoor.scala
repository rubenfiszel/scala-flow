package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object ParticleFilterOutdoor extends FlowApp[Trajectory] {

  //****** Model ******

  val dtIMU  = 0.001
  val dtAlt  = (dtIMU * 10) + dtIMU * 2
  val dtGPS1 = (dtIMU * 500) + dtIMU * 10
  val dtGPS2 = (dtIMU * 500) + dtIMU * 310

  val covScale = 10.0
  //filter parameter
  val covAcc  = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covGyro = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covMag = DenseMatrix.eye[Real](3) * (0.1 * covScale)  
  val varAlt  = 0.1 * covScale
  val covGPS1 = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covGPS2 = DenseMatrix.eye[Real](3) * (0.1 * covScale)

  val numberParticles = 200

  val initQ = Quaternion(1.0, 0, 0, 0)

  val clockIMU  = new TrajectoryClock(dtIMU)
  val clockAlt  = new TrajectoryClock(dtAlt)
  val clockGPS1 = new TrajectoryClock(dtGPS1)
  val clockGPS2 = new TrajectoryClock(dtGPS2)

  val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

  val imu  = clockIMU.map(IMU(covAcc, covGyro, dtIMU))
  val mag = clockIMU.map(Magnetometer(covMag))
  val gps1 = clockGPS1.map(GPS(covGPS1))
  val gps2 = clockGPS2.map(GPS(covGPS2))
  val alt  = clockAlt.map(Altimeter(varAlt))  

  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")
  Plot2(batch, accelerometer)
   */

  val filter = ParticleFilterMag2GPSAlt(
    imu,
    mag,
    gps1,
    gps2,
    alt,
    initQ,
    numberParticles,
    covAcc,
    covGyro,
    covMag,
    covGPS1,
    covGPS2,
    varAlt
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
