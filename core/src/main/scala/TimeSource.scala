package dawn.flow

import breeze.stats.distributions._

class TimeSource(source: Source[Time]) {

  def latency(dt: Timestep) =
    source.map((x: Time) => x+dt, "Latency")

  def latencyVariance(std: Real) =
    source.map((x: Time) => Gaussian(x, std)(Random).draw(), "ClockVar " +std)

  def stop(tf: Timeframe) =
    source.takeWhile(_ < tf, "Stop")

  def accumulate[A](source1: SourceT[A]) =
    Accumulate(source, source1)

  def combine[A, C](source2: SourceT[A], source3: SourceT[C]) =
    Combine(source, source2, source3)

  def synchronize[A, C](source2: SourceT[A], source3: SourceT[C], reduce2: (Timestamped[A], Timestamped[A]) => Timestamped[A], reduce3: (Timestamped[C], Timestamped[C]) => Timestamped[C], default2: A, default3: C) = {
    def streamOrElse[D](t: Time, stream: StreamT[D], default: D) =
      if (stream.isEmpty)
        Stream(Timestamped(t, default))
      else
        stream

    combine(source2, source3).zip(source).map(x => (streamOrElse(x._2, x._1._1, default2).reduce(reduce2), streamOrElse(x._2, x._1._2, default3).reduce(reduce3)))
  }
  

}
