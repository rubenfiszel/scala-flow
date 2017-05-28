package dawn.flow

import breeze.linalg._
import breeze.stats.distributions._

object Rand {
  def gaussian(v: VectorR, cov: MatrixR): VectorR =
    MultivariateGaussian(v, cov)(Random).draw()

  def gaussian(v: Real, std: Real): Real =
    Gaussian(v, std)(Random).draw()

  def uniform() =
    Uniform(0, 1)(Random).draw()
}
