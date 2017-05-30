package dawn.flow

//import breeze.stats.distributions._

class TimeSource(source: Source[Time]) {

  /*
  def latencyVariance(std: Real) =
    source.map((x: Time) => Gaussian(x, std)(Random).draw(), "ClockVar " +std)

  def stop(tf: Timeframe) =
    source.takeWhile(_ < tf, "Stop")
   */

  def latency(dt: Time) =
    new Op1[Time, Time] {
      def rawSource1 = source
      def listen1(x: Time) = 
        broadcast(x + dt, dt)
      def name = "Latency " + dt
    }

}
