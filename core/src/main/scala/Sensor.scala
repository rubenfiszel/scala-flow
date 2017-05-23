package dawn.flow

import breeze.linalg._
import breeze.stats.distributions._

object Rand {
  def gaussian(v: Vec3, cov: DenseMatrix[Real]): Vec3 =
    Vec3(MultivariateGaussian(v, cov)(Random).draw().toArray)

  def gaussian(v: Real, std: Real): Real =
    Gaussian(v, std)(Random).draw()
  
}


trait Sensor[A, M] extends ((M, Time) => Timestamped[A]) {

  override def toString = getClass.getSimpleName

  def generate(model: M, t: Time): A

  def apply(p: M, t: Time) =
    Timestamped(t, generate(p, t))
}

trait VectorSensor[M] extends Sensor[Vec3, M] {

  def cov: DenseMatrix[Real]

  def genVector(p: M, t: Time): Vec3

  def generate(p: M, t: Time) =
    Rand.gaussian(genVector(p, t), cov)  //WITH NOISE
//    genVector(p, t) //NO NOISE


}

