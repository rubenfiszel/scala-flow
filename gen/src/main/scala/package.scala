package spatial.fusion

package object gen {
  type Real      = Double
  type State     = Seq[Real]
  type Seed      = Long
  type Timestep  = Real
  type Timeframe = Real
  type Time      = Real
  type Thrust    = Real
  type Omega     = Real
  def toReal(x: Timestep) = x.toDouble
}
