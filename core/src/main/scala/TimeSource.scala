package dawn.flow

//import breeze.stats.distributions._

class TimeSource(source: Source[Time]) {


  def latencyVariance(std: Real) =
    source.map((x: Time) => Rand.gaussian(x, std), "ClockVar " +std)

  def stop(tf: Timeframe) =
    source.takeWhile(_ < tf, "Stop")


  def latency(dt: Time) =
    new Op1[Time, Time] {
      def rawSource1 = source
      def listen1(x: Timestamped[Time]) = 
        broadcast(x.addLatency(x.dt), x.dt)
      def name = "Latency " + dt
    }

}
