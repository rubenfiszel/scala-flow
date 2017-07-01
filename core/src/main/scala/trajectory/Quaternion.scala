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

  def distance(q1: Quat, q2: Quat) = 6.0 - 2*trace(q1.attitudeMatrix*q2.attitudeMatrix.t)

  def getQuaternion(v1: Vec3, v2: Vec3) = {
    //http://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    val a = cross(v1, v2)
    val w = sqrt((norm(v1) ** 2) * (norm(v2) ** 2)) + v1.dot(v2)
    Quaternion(w, a.x, a.y, a.z).normalized
  }

  //http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle/index.htm
  def quatToAxisAngle(qstart: Quat, qend: Quat) = {
    val q = qstart.reciprocal * qend
    val v = Vec3(q.i, q.j, q.k)
    //floating point error seems to give q.r > 1 sometimes which fail acos
    val angle = 2 * acos(Math.min(q.r, 1.0))
    normalize(v) * angle
  }

  def quatToAngle(q: Quat): Vec3 = {
    val n = acos(q.r)*2
    val s = n/sin(n/2)
    Vec3(q.i*s, q.j*s, q.k*s) 
  }

  def inverseQuatIfNotClose(ps: Seq[(Real, Quat)]): Seq[(Real, Quat)] = {
    val first = ps(0)._2.toDenseVector    
    def inverseIfNotClose(x: Quat) = 
      if (x.toDenseVector.dot(first) > 0.0)
        x
      else
        -x

    ps.map(x => (x._1, inverseIfNotClose(x._2)))

  }
  
  //https://stackoverflow.com/questions/12374087/average-of-multiple-quaternions
  def averageQuaternion(sqdv: Seq[(Real, Quat)]): Quat = {

    //Should be the right way but doesn't seem to work as good as the second technique
//    /*
//    val sqdv = ps.sp.map(x => (x.w, x.q.toDenseVector))
    val qr = DenseMatrix.zeros[Real](4, 4)
    for ((w, qdv) <- sqdv) {
      val q = qdv.toDenseVector
      qr += (q*q.t)*w
    }
    val eg = eig(qr)
    val pvector = eg.eigenvectors(::, 0)
    val r = pvector.toQuaternion

    if (r.r < 0) {
      //fallback method if absurd quaternion
      //Use this instead http://wiki.unity3d.com/index.php/Averaging_Quaternions_and_Vectors

      (inverseQuatIfNotClose(sqdv)
        .foldLeft(Quaternion(0.0, 0.0, 0.0, 0.0))((acc, qw) => acc + qw._2 * qw._1))
        .normalized
    }
    else
      r
  }



  //https://www.astro.rug.nl/software/kapteyn/_downloads/attitude.pdf page 19
  def bodyRateToGlobalQuat(q: Quat,
                           omega: Vec3): Quat = {
    val omegaQ = Quaternion(0.0, omega.x, omega.y, omega.z) * (1.0 / 2)
    q * omegaQ
  }

  
  def localAngleToLocalQuat(angle: Vec3): Quat = {
    val l = norm(angle) / 2
    val nrot = normalize(angle) * sin(l)
    Quaternion(cos(l), nrot.x, nrot.y, nrot.z)
  }

}
