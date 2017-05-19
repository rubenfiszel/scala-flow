package dawn.flow

import breeze.stats.distributions._

class TimeSource[B](source: Source[Time, B]) {

  def latency(dt: Timestep) =
    source.map((x: Time) => x+dt, "Latency")

  def latencyVariance(std: Real) =
    source.map((x: Time) => Gaussian(x, std)(Random).draw(), "ClockVar " +std)

  def stop(tf: Timeframe) =
    source.takeWhile(_ < tf, "Stop")

}
