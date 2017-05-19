package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg.{max => _, min => _, _ => _}
import spire.math.{Real => _, _ => _}
import spire.implicits._

//Trajectory Quaternion to avoid namespace conflict with spire Quaternion
object TQuaternion {

  def getQuaternion(v1: Vec3, v2: Vec3) = {
    //http://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    val a     = Vec3(cross(v1, v2))
    val w     = sqrt((norm(v1) ** 2) * (norm(v2) ** 2)) + v1.dot(v2)
    Quaternion(w, a.x, a.y, a.z).normalized
  }

  //http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle/index.htm
  def quatToBodyRate(qstart: Quaternion[Real], qend: Quaternion[Real]) = {
    val q     = qstart.reciprocal * qend
    val v     = Vec3(q.i, q.j, q.k)
    val angle = 2 * acos(q.r)
    Vec3(normalize(v)*angle)
  }

  //https://www.astro.rug.nl/software/kapteyn/_downloads/attitude.pdf page 19
  def bodyRateToQuat(q: Quaternion[Real], omega: Vec3): Quaternion[Real] = {
    val omegaQ = Quaternion(0.0, omega.x, omega.y, omega.z) * (1.0 / 2)
    q * omegaQ
  }

}
