package dawn.flow

import breeze.linalg._
import breeze.stats.distributions._

object Rand {
  def gaussian(m: VectorR, cov: MatrixR): VectorR =
    MultivariateGaussian(m, cov)(Random).draw()

  def gaussianLogPdf(v: VectorR, m: VectorR, cov: MatrixR): Real =
    MultivariateGaussian(m, cov).logPdf(v)
  
  def gaussian(m: Real, variance: Real): Real =
    Gaussian(m, variance)(Random).draw()

  def gaussianLogPdf(v: Real, m: Real, variance: Real): Real =
    Gaussian(m, variance).logPdf(v)

  def uniform() =
    Uniform(0, 1)(Random).draw()
}
