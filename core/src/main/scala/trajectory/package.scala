package dawn.flow

import io.circe.generic.semiauto._
import io.circe._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg.{DenseVector, DenseMatrix, norm}

package object trajectory {

  implicit class DenseVectorOps(v: VectorR) {
    def toQuaternion = Quaternion(v(0), v(1), v(2), v(3))
  }

  implicit class QuaternionOps(q: Quaternion[Real]) {
    def normalized = {
      val normQ = sqrt(q.r ** 2 + q.i ** 2 + q.j ** 2 + q.k ** 2)
      q / normQ
    }

    //https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation
    def rotate(v: Vec3) = {
      val qga = Quaternion(0.0, v.x, v.y, v.z)
      val rq = q * qga * q.reciprocal
      Vec3(rq.i, rq.j, rq.k)
    }

    def rotateBy(q2: Quaternion[Real]) =
      q2 * q

    def toDenseVector =
      DenseVector(q.r, q.i, q.j, q.k)

    //https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
    def getPitch =
      asin(2*(q.r*q.j - q.k*q.i))

    //https://ntrs.nasa.gov/archive/nasa/casi.ntrs.nasa.gov/20070017872.pdf PAGE 
    def attitudeMatrix = {
      val axisC = DenseVector(q.i, q.j, q.k)

      val m = DenseMatrix(
        (0.0, -q.k, q.j),
        (q.k, 0.0, -q.i),
        (-q.k, q.i, 0.0))

      DenseMatrix.eye[Real](3)*(q.r - norm(axisC)**2) + axisC*axisC.t*2.0 - m*2.0*q.r

    }

    //https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation#Quaternion-derived_rotation_matrix
    def rotationMatrix = 
      DenseMatrix(
        (1.0-2.0*(q.j**2 + q.k**2), 2.0*(q.i*q.j-q.k*q.r), 2.0*(q.i*q.k + q.j*q.r)),
        (2.0*(q.i*q.j+q.k*q.r), 1.0-2.0*(q.i**2 + q.k**2), 2.0*(q.j*q.k-q.i*q.r)),
        (2.0*(q.i*q.k-q.j*q.r), 2.0*(q.j*q.k + q.i*q.r), 1.0-2.0*(q.i**2 + q.j**2)))

    //https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5108752/
      def riemannMatrix = 
        DenseMatrix(
          (q.r, -q.k, q.j),
          (q.k, q.r, -q.i),
          (-q.j, q.i, q.r),
          (-q.i, -q.j, -q.k)
        )
  }

}
