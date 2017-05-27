package dawn.flow

import breeze.linalg._
import breeze.stats.distributions._

object Rand {
  def gaussian(v: Vec3, cov: DenseMatrix[Real]): Vec3 =
    Vec3(MultivariateGaussian(v, cov)(Random).draw().toArray)

  def gaussian(v: Real, std: Real): Real =
    Gaussian(v, std)(Random).draw()

  def uniform() =
    Uniform(0, 1)(Random).draw()
}
