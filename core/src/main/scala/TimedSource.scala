package dawn.flow

class TimedSource[A, B](source: Source[Timestamped[A], B]) {

  def mapT[C](f: (B, A) => C) =
    source.map(
      NamedFunction2((p: B, x: Timestamped[A]) => x.copy(v = f(p, x.v)),
                     "Functor2 " + f.toString))

  def mapT[C](f: (A) => C) =
    source.map(
      NamedFunction1((x: Timestamped[A]) => x.copy(v = f(x.v)),
                     "FunctorT " + f.toString))

  def latency(dt1: Timestep) =
    source.map(
      NamedFunction1((x: Timestamped[A]) => x.copy(dt = x.dt + dt1),
                     "Latency " + dt1))

  def buffer(time: Source[Time, B]) =
    Buffer(time, source)

  def combine[C](time: Source[Time, B], source2: SourceT[C, B]) =
    Combine(time, source, source2)

}
