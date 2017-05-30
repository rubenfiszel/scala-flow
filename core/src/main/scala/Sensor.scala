package dawn.flow

import breeze.linalg._

trait Sensor[A, M] extends (Time => A) with RequireModel[M] {

  override def toString = getClass.getSimpleName

  def generate(model: M, t: Time): A

  def apply(t: Time) =
    generate(model.get, t)
}

trait VectorSensor[M] extends Sensor[Vec3, M] {

  def cov: DenseMatrix[Real]

  def genVector(p: M, t: Time): Vec3

  def generate(p: M, t: Time) =
    Rand.gaussian(genVector(p, t), cov) //WITH NOISE
//    genVector(p, t) //NO NOISE

}
