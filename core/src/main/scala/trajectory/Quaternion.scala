package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg.{max => _, min => _, _ => _}
import spire.math.{Real => _, _ => _}
import spire.implicits._

//Trajectory Quaternion to avoid namespace conflict with spire Quaternion
object Quat {

  def genQuaternion(mean: Quat, cov: MatrixR) = {
    val error =  Rand.gaussian(DenseVector(0.0, 0, 0), cov)
    val eQ = localAngleToLocalQuat(error)
    mean*eQ
  }

  def getQuaternion(v1: Vec3, v2: Vec3) = {
    //http://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    val a = cross(v1, v2)
    val w = sqrt((norm(v1) ** 2) * (norm(v2) ** 2)) + v1.dot(v2)
    Quaternion(w, a.x, a.y, a.z).normalized
  }

  //http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle/index.htm
  def quatToAxisAngle(qstart: Quaternion[Real], qend: Quaternion[Real]) = {
    val q = qstart.reciprocal * qend
    val v = Vec3(q.i, q.j, q.k)
    //floating point error seems to give q.r > 1 sometimes which fail acos
    val angle = 2 * acos(Math.min(q.r, 1.0))
    normalize(v) * angle
  }

  //https://www.astro.rug.nl/software/kapteyn/_downloads/attitude.pdf page 19
  def bodyRateToGlobalQuat(q: Quaternion[Real],
                           omega: Vec3): Quaternion[Real] = {
    val omegaQ = Quaternion(0.0, omega.x, omega.y, omega.z) * (1.0 / 2)
    q * omegaQ
  }

  
  def localAngleToLocalQuat(angle: Vec3): Quaternion[Real] = {
    val l = norm(angle) / 2
    val nrot = normalize(angle) * sin(l)
    Quaternion(cos(l), nrot.x, nrot.y, nrot.z)
  }

}
