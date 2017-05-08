package spatial.fusion

import breeze.stats.distributions._

package object gen {
  type Real      = Double
  type State     = Seq[Real]
  type Seed      = Int
  type Timestep  = Real
  type Timeframe = Real
  type Time      = Real
  type Thrust    = Real
  type Omega     = Real
  def toReal(x: Timestep) = x.toDouble

  type Goal = Keypoint

  type Rate = Long

  //variance in time prediction (simulate imperfection of reality)
  def genTimes(dt: Timestep, tf: Timeframe, variance: Double, seed: Seed): Stream[Time] = {
    val u = Gaussian(0, variance*dt)(RandBasis.withSeed(seed))
    genPerfectTimes(dt, tf).map(_ + u.draw())
  }

  def genPerfectTimes(dt: Timestep, tf: Timeframe): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0).takeWhile(_ < tf)
  }

  def fromRate(i: Long): Timestep = 1.0 / i

}
