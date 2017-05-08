package spatial.fusion.gen


import spire.math._
import spire.implicits._
import breeze.linalg._
import breeze.stats.distributions._

object Constants {

  final val g = 9.87

}
trait Universe {
  def reset(seed: Seed): State
  def update(dt: Timestep): State
  def state: State
}

trait QuadCopter {

  final val THRUST_MEAN = 0
  final val THRUST_SIG = 0.2

  final val ROT_MEAN = 0
  final val ROT_SIG = 0.2

  var p = DenseVector[Real](0, 0, 0)
  var v = DenseVector[Real](0, 0, 0)
  var lastAcc = DenseVector[Real](0, 0, 0)

  var attitude = Quaternion[Real](0, 0, 0, 0)
  var attitudeV = Quaternion[Real](0, 0, 0, 0)
  var lastAttitudeAcc = Quaternion[Real](0, 0, 0, 0)

  def reset(seed: Seed) = {
    p = DenseVector[Real](0, 0, 0)
    v = DenseVector[Real](0, 0, 0)
    lastAcc = DenseVector[Real](0, 0, 0)

    attitude = Quaternion[Real](0, 0, 0, 0)
    attitudeV = Quaternion[Real](0, 0, 0, 0)
    lastAttitudeAcc = Quaternion[Real](0, 0, 0, 0)

  }

  def applyLinAcc(dt: Timestep, ax: Real, ay: Real, az: Real) = {
    lastAcc = DenseVector(ax, ay, az)
    //Do I apply velocity before or after modifying position ?
    //Best guess it to ignore velocity change during that timestep else
    //I would technically also have to integrate over the angular change
    p += v * toReal(dt)
    v += lastAcc * toReal(dt)
  }

  def applyAngAcc(dt: Timestep, ax: Real, ay: Real, az: Real) = {
    lastAttitudeAcc = Quaternion[Real](0, 0, 0, 0)
//    attitudeV += lastAcc * toReal(dt)
//    p += v * toReal(dt)
  }

  def genThrust: Real = {
    val g = Gaussian(0, 1)
    g.sample()
  }

  def genRot: DenseVector[Real] = {
    val u = Uniform(0, 1)
    val v = DenseVector(u.sample(3).toArray)
    normalize(v)
  }

  def update(dt: Timestep): State = {
    val thrust = genThrust
    val rotatedThrust: IndexedSeq[Real] = ??? //rotate(attitude, thrust)
    val rot = genRot
    applyLinAcc(dt, rotatedThrust(0), rotatedThrust(1), rotatedThrust(2))
    applyAngAcc(dt, rot(0), rot(1), rot(2))
    p.toArray.toSeq ++ v.toArray.toSeq ++ lastAcc.toArray.toSeq
  }

  def toQuaternion(a: Real, b: Real, c: Real) = {
    //https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
    val r3 = new DenseMatrix(4, 1, Array[Real](cos(a / 2), 0, 0, sin(a / 2)))
    val r2 = new DenseMatrix(4, 1, Array[Real](cos(b / 2), 0, sin(b / 2), 0))
    val r1 = new DenseMatrix(4, 1, Array[Real](cos(c / 2), sin(c / 2), 0, 0))
    val r = r3 * r2 * r1
    Quaternion(r(0, 0), r(1, 0), r(2, 0), r(3, 0))
  }

}

trait Bezier {}
