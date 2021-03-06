package dawn.flow

//import breeze.stats.distributions._

class TimeSource(source: Source[Time]) {

  def stop(tf: Timeframe) =
    source.takeWhile(_ < tf, "Stop")

  def latencyVariance(mean: Real, std: Real) =
    new Op1[Time, Time] {
      def rawSource1 = source
      def listen1(x: Timestamped[Time]) = {
        val delay = Rand.gaussian(mean, std)
        broadcast(x.addLatency(delay), delay)
      }
      def name = "LatencyVar " + mean + " " + std
    }

  def latency(dt: Time) =
    new Op1[Time, Time] {
      def rawSource1 = source
      def listen1(x: Timestamped[Time]) =
        broadcast(x.addLatency(dt), dt)
      def name = "Latency " + dt
    }

  def deltaTime(init: Time = 0.0): Source[Time] = {
    lazy val buffer = Buffer[Time](source, init, source)
    source
      .zip(buffer)
      .map(x => x._2 - x._1)    
  }

}
